package mes.app.precedence.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import mes.domain.entity.User;
import mes.domain.services.SqlRunner;

@Service
public class PartnerCheckService {
	
	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getList(String start_date, String end_date) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("start_date", start_date);
		paramMap.addValue("end_date", end_date);
		
		
		
		String sql = """
				select 			
				 b.id, b."Char1" as "Title", coalesce(r."StateName", '작성') as "StateName", r."LineName"
				 , r."LineNameState", FORMAT(b."Date1", 'yyyy-MM-DD') as "DataDate"
				 , coalesce(r."SearchYN", 'Y') as "SearchYN", coalesce(r."EditYN", 'Y') as "EditYN"
				 , coalesce(r."DeleteYN", 'Y') as "DeleteYN", b."Number1" as check_master_id
				 ,b._creater_id ,up."Name" as creater_name , b._modifier_id, up2."Name" as modifier_name
				 , cm."CheckCycle" , b."Text1"
				 , tir."InputResult" as partner_type
				 , sum(cast(tir."Char1" as float)) as total_num
				from bundle_head b                               
				left join v_appr_result r on b.id = r."SourceDataPk" and r."SourceTableName" = 'bundle_head'
				left join user_profile up on b._creater_id = up."User_id"
				left join user_profile up2 on b._modifier_id = up2."User_id"
				left join check_mast cm on cm.id = b."Number1"
				left join test_result tr on tr."SourceDataPk" = b.id 
				left join test_item_result tir on tir."TestResult_id"  = tr.id
				where b."TableName" = 'partner_check'
				and b."Date1" between cast(:start_date as date) and cast(:end_date as date)  
				group by b.id,r."StateName",r."LineName" ,r."LineNameState", r."SearchYN", r."EditYN" , r."DeleteYN", up."Name" ,up2."Name" , cm."CheckCycle" , tir."InputResult" 
				""";
		
		
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;	
	}

	public Map<String, Object> apprList(Integer bhId, String data_date, Authentication auth) {
		User user = (User)auth.getPrincipal();
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("data_date", data_date);
		paramMap.addValue("bhId", bhId);	
		
		String sql = "";
		
		
		Map<String, Object> head_info = new HashMap<>();	
		
		List<Map<String, Object>> diary_info = null;
		
		if(bhId > 0) {
		 sql = """
					select bh.id
	                , bh."Char1" as "Title"
	                , bh."Char2" as "Description"
	                , FORMAT(bh."Date1", 'yyyy-MM') as "DataDate"
	                , coalesce(uu."Name", cu."Name") as "FirstName"
	                , coalesce(r."State", 'write') as "State"
	                , coalesce(r."StateName", '작성') as "StateName"
	                from bundle_head bh
	                inner join user_profile cu on bh._creater_id = cu."User_id"
	                left join user_profile uu on bh._modifier_id = uu."User_id"
	                left join v_appr_result r on bh.id = r."SourceDataPk" and r."SourceTableName" = 'bundle_head'
	                where bh."TableName" = 'partner_check'
	                and bh.id = :bhId
					""";
				
				head_info = this.sqlRunner.getRow(sql, paramMap);
				
				sql = """
					select tir."CharResult",tir."Char1",tir."Char2",bh."Char1",FORMAT(bh."Date1",'YYYY-MM-DD')as date1,bh."Text1",
	                ti."Name", FORMAT(tir."TestDateTime",'YYYY-MM-DD')as testDateTime ,bh."Char2" as necessity,
					tim."SpecType" ,tim."SpecText" ,tim."LowSpec" ,tim."UpperSpec", tir."TestItem_id" ,bh.id, ti."ItemType",tir."InputResult"
					from test_item_result tir 
					inner join test_result tr on tr.id = tir."TestResult_id"
					inner join bundle_head bh on tr."SourceDataPk" = bh.id
					inner join test_item ti on ti.id = tir."TestItem_id"
					left join test_item_mast tim on tim."TestMaster_id" = tr."TestMaster_id" and tim."TestItem_id" = ti.id
					where 1=1
					and bh.id = :bhId
					order by tir.id
						""";
				
				diary_info = this.sqlRunner.getRows(sql, paramMap);
				
		}else {
			head_info.put("id", 0);
			head_info.put("Title", "협력업체점검표");
			head_info.put("DataDate", data_date);
			head_info.put("FirstName", user.getUserProfile().getName());
			head_info.put("StateName", "상신대기");
			head_info.put("check_result_id", 0);
			head_info.put("Description", "");
			head_info.put("CheckStep", 1);
			
			
			sql = """
					select tim.*,ti."Name", '' as "Char2", '' as "Char1",ti."ItemType"
					from test_item_mast tim 
					inner join test_mast tm on tim."TestMaster_id"  = tm.id 
					inner join test_item ti on tim."TestItem_id"  = ti.id
					where tm."Name"  = '협력업체점검표'
					""";
			
			
			diary_info = this.sqlRunner.getRows(sql, null);
			
		}
		
		
		Map<String,Object> item = new HashMap<>();
		item.put("diary_info",diary_info);
		item.put("head_info",head_info);
		
		
		return item;
	}

	public void mstDelete(Integer bhId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("bhId", bhId);
		String sql = "";
		
		 sql = """
				delete from test_item_result where 1=1 and "SourceTableName" = 'partner_check' and "SourceDataPk" = :bhId
				""";
		
		 sql = """
		 		delete from test_result where 1=1 and "SourceTableName"  = 'partner_check' and "SourceDataPk" = :bhId
		 		""";
		 
		this.sqlRunner.execute(sql, paramMap);
		
		 sql = """
				delete from bundle_head 
				where 1=1
				and "TableName" = 'partner_check'
				and id = :bhId
				""";
		
		this.sqlRunner.execute(sql, paramMap);		
	}

}
