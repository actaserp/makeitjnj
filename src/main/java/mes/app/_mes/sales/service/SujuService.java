package mes.app.sales.service;

import java.util.List;
import java.util.Map;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import mes.domain.entity.Suju;
import mes.domain.repository.SujuRepository;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;

@Service
public class SujuService {

	@Autowired
	SqlRunner sqlRunner;
	
	@Autowired
	SujuRepository SujuRepository;
	
	
	// 수주 내역 조회 
	public List<Map<String, Object>> getSujuList(String date_kind, Timestamp start, Timestamp end) {
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("date_kind", date_kind);
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		
		String sql = """
			select s.id
            , s."JumunNumber"
            , s."Material_id" as "Material_id"
            , mg."Name" as "MaterialGroupName"
            , mg.id as "MaterialGroup_id"
            , dbo.fn_code_name('mat_type', mg."MaterialType") as "MaterialTypeName"
            , m.id as "Material_id"
            , m."Code" as product_code
            , m."Name" as product_name
            , u."Name" as unit
            , s."SujuQty" as "SujuQty"
            , FORMAT(s."JumunDate", 'yyyy-mm-dd') as "JumunDate"
            , FORMAT(s."DueDate", 'yyyy-mm-dd') as "DueDate"
            , s."CompanyName"
            , s."Company_id"
            , s."SujuType"
            , dbo.fn_code_name('suju_type', s."SujuType") as "SujuTypeName"
            , FORMAT(s."ProductionPlanDate", 'yyyy-mm-dd') as production_plan_date
            , FORMAT(s."ShipmentPlanDate", 'yyyy-mm-dd') as shiment_plan_date
            , s."Description"
            , s."AvailableStock" as "AvailableStock"
            , s."ReservationStock" as "ReservationStock"
            , s."SujuQty2" as "SujuQty2"
            , dbo.fn_code_name('suju_state', s."State") as "StateName"
            , dbo.fn_code_name('shipment_state', s."ShipmentState") as "ShipmentStateName"
            , s."State"
            , FORMAT(s."_created", 'yyyy-mm-dd') as create_date
            , case s."PlanTableName" when 'prod_week_term' then '주간계획' when 'bundle_head' then '임의계획' else s."PlanTableName" end as plan_state
            from suju s
            inner join material m on m.id = s."Material_id"
            inner join mat_grp mg on mg.id = m."MaterialGroup_id"
            left join unit u on m."Unit_id" = u.id
            left join company c on c.id= s."Company_id"
            where 1 = 1
			""";
		
		if (date_kind.equals("sales")) {
			sql += """
				and s."JumunDate" between :start and :end 
		        order by s."JumunDate" desc,  m."Name"
				""";
		} else {
			sql +="""
				and s."DueDate" between :start and :end
		        order by s."DueDate" desc,  m."Name"
				""";
		}
		
		List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);
		
		return itmes;
	}
	
	// 수주 상세정보 조회
	public Map<String, Object> getSujuDetail(int id) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", id);
		
		String sql = """
			select s.id
            , s."JumunNumber"
            , s."Material_id" as "Material_id"
            , mg."Name" as "MaterialGroupName"
            , mg.id as "MaterialGroup_id"
            , dbo.fn_code_name('mat_type', mg."MaterialType") as "MaterialTypeName"
            , m.id as "Material_id"
            , m."Code" as product_code
            , m."Name" as product_name
            , u."Name" as unit
            , s."SujuQty" as "SujuQty"
            , FORMAT(s."JumunDate", 'yyyy-mm-dd') as "JumunDate"
            , FORMAT(s."DueDate", 'yyyy-mm-dd') as "DueDate"
            , s."CompanyName"
            , s."Company_id"
            , s."SujuType"
            , dbo.fn_code_name('suju_type', s."SujuType") as "SujuTypeName"
            , FORMAT(s."ProductionPlanDate", 'yyyy-mm-dd') as production_plan_date
            , FORMAT(s."ShipmentPlanDate", 'yyyy-mm-dd') as shiment_plan_date
            , s."Description"
            , s."AvailableStock" as "AvailableStock"
            , s."ReservationStock" as "ReservationStock"
            , s."SujuQty2" as "SujuQty2"
            , s."State"
            , dbo.fn_code_name('suju_state', s."State") as "StateName"
            , FORMAT(s."_created", 'yyyy-mm-dd') as create_date
            from suju s
            inner join material m on m.id = s."Material_id"
            inner join mat_grp mg on mg.id = m."MaterialGroup_id"
            left join unit u on m."Unit_id" = u.id
            left join company c on c.id= s."Company_id"
            where s.id = :id
			""";
		
		Map<String,Object> item = this.sqlRunner.getRow(sql, paramMap);
		
		return item;
	}
	
	// 제품 정보 조회
	public Map<String, Object> getSujuMatInfo(int product_id) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("product_id", product_id);
		
		String sql = """
			select m.id as mat_pk
			, m."AvailableStock" 
			, u."Name" as unit_name
			from material m 
			inner join unit u on u.id = m."Unit_id" 
			where m.id = :product_id
			""";
		
		Map<String, Object> item = this.sqlRunner.getRow(sql, paramMap);
		
		return item;
	}
	
	public String makeJumunNumber(Date dataDate) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("data_date", dataDate);
		
		String jumunNumber = "";
		
		String sql = """
		select "CurrVal" from seq_maker where "Code" = 'JumunNumber' and "BaseDate" = :data_date
		""";
		Map<String, Object> mapRow = this.sqlRunner.getRow(sql, paramMap);
		
		int currVal = 1;
		if (mapRow!=null && mapRow.containsKey("CurrVal")) {
			currVal =  (int)mapRow.get("CurrVal");
			sql = """
		    update seq_maker set "CurrVal" = "CurrVal" + 1, "_modified" = now()	where "Code" = 'JumunNumber' and "BaseDate" = :data_date
			""";
			this.sqlRunner.execute(sql, paramMap);
		}else {
			sql = """
			insert into seq_maker("Code", "BaseDate", "CurrVal", "_modified") values('JumunNumber', :data_date, 1, now());	
			""";
			this.sqlRunner.execute(sql, paramMap);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		jumunNumber = String.format("{0}-{1}", sdf.format(dataDate), currVal);
		return jumunNumber;	
	}
	
	public String makeJumunNumberAndUpdateSuju(int suju_id, String dataDate) {

		Suju suju = this.SujuRepository.getSujuById(suju_id);
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("data_date", dataDate);
		
		String jumunNumber = suju.getJumunNumber();
		if(StringUtils.hasText(jumunNumber)==false) {
			Date jumun_date = CommonUtil.trySqlDate(dataDate);
			jumunNumber = this.makeJumunNumber(jumun_date);
			suju.setJumunNumber(jumunNumber);
			this.SujuRepository.save(suju);
		}
		return jumunNumber;
	}
}
