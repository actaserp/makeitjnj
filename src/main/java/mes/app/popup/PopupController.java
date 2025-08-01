package mes.app.popup;


import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.domain.model.AjaxResult;
import mes.domain.services.DateUtil;
import mes.domain.services.SqlRunner;

@Slf4j
@RestController
@RequestMapping("/api/popup")
public class PopupController {
	
	@Autowired
	SqlRunner sqlRunner;
	
	@RequestMapping("/search_material")
	public AjaxResult getSearchMaterial(
			@RequestParam(value="material_type", required=false) String material_type,
			@RequestParam(value="material_group", required=false) Integer material_group,
			@RequestParam(value="keyword", required=false) String keyword
			) {
		AjaxResult result = new AjaxResult();
		
		String sql ="""
	            select 
	            m.id
	            , m."Code"
	            , m."Name"
	            , m."MaterialGroup_id"
	            , mg."Name" as group_name
	            , mg."MaterialType"
	            , sc."Value" as "MaterialTypeName"
	            , sc."Code" as "MaterialTypeCode"
	            , u."Name" as unit_name
	            from material m
	            left join unit u on m."Unit_id" = u.id
	            left join mat_grp mg on m."MaterialGroup_id" = mg.id
	            left join sys_code sc on mg."MaterialType" = sc."Code" 
	            and sc."CodeType" ='mat_type'
	            where 1=1
	    """;

		if (StringUtils.hasText(material_type)){
            sql+=""" 
            and mg."MaterialType" =:material_type
            """;
		}

		if(material_group!=null){
            sql+="""            		
            and mg."id" =:material_group
            """;	
		}

		if(StringUtils.hasText(keyword)){
            sql+="""
            AND (m.[Name] LIKE '%' + :keyword + '%' OR m.[Code] LIKE '%' + :keyword + '%')
            """;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("material_type", material_type);
		paramMap.addValue("material_group", material_group, java.sql.Types.INTEGER);
		paramMap.addValue("keyword", keyword);
		result.data = this.sqlRunner.getRows(sql, paramMap);
		log.info("read SQL: {}", sql);
    log.info("SQL Parameters: {}", paramMap.getValues());
		return result;
	}	
	
	
	@RequestMapping("/search_equipment")
	public AjaxResult getSearchMaterial(
			@RequestParam(value="group_id", required=false) Integer equipment_group,			
			@RequestParam(value="keyword", required=false) String keyword			
			) {
		AjaxResult result = new AjaxResult();
		
		String sql ="""
	            select 
                 e.id
                 , e."Code"
                 , e."Name"
                 , eg."Name" as group_name
                 , eg."EquipmentType"
                 , dbo.fn_code_name('equipment_type',  eg."EquipmentType") as "EquipmentTypeName"
                from equ e
                  left join equ_grp eg on e."EquipmentGroup_id" = eg.id
                where 1=1  
	    """;
		
		if(equipment_group!=null){
            sql+="""            		
            and e."EquipmentGroup_id"=:equipment_group
            """;	
		}
		
		if(StringUtils.hasText(keyword)){
            sql+="""
            and upper(e."Name") like concat('%%',:keyword,'%%')
            """;		
		}
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("equipment_group", equipment_group, java.sql.Types.INTEGER);
		paramMap.addValue("keyword", keyword);
		
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;		
	}
	
	@RequestMapping("/pop_prod_input/mat_list")
	public AjaxResult getMatList(
			@RequestParam(value="mat_type", required=false) String matType,
			@RequestParam(value="mat_grp_id", required=false) Integer matGrpId,
			@RequestParam(value="keyword", required=false) String keyword,
			@RequestParam(value="jr_pk", required=false) Integer jrPk,
			@RequestParam(value="bom_comp_yn", required=false) String bomCompYn) {
		
		AjaxResult result = new AjaxResult();
		
		Timestamp today = DateUtil.getNowTimeStamp();
		
		String sql = "";
		
		if (bomCompYn.equals("Y")) {
			sql = """
				 select m.id, m."Code" as mat_code, m."Name" as mat_name
                , m."MaterialGroup_id" as mat_grp_id, mg."Name" as mat_grp_name
                , mg."MaterialType" as mat_type
                , sc."Value" as mat_type_name
                , u."Name" as unit_name
                from job_res jr
                inner join tbl_bom_detail(jr."Material_id"::text, cast(FORMAT(cast(:today as date),'YYYY-MM-DD') as text)) a  on a.b_level = 1 
                inner join material m on m.id = a.mat_pk
                left join unit u on m."Unit_id" = u.id
                left join mat_grp mg on m."MaterialGroup_id" = mg.id
                left join sys_code sc on mg."MaterialType" = sc."Code" 
                and sc."CodeType" ='mat_type'
                where jr.id =  :jrPk
                and m."LotUseYN" = 'Y'
				 """;
			
			MapSqlParameterSource paramMap = new MapSqlParameterSource();
			paramMap.addValue("jrPk", jrPk);
			paramMap.addValue("today", today);
			
			result.data = this.sqlRunner.getRows(sql, paramMap);
			
		} else {
			sql = """
					select m.id, m."Code" as mat_code, m."Name" as mat_name
	                , m."MaterialGroup_id" as mat_grp_id, mg."Name" as mat_grp_name
	                , mg."MaterialType" as mat_type
	                , sc."Value" as mat_type_name
	                , u."Name" as unit_name
	                from material m
	                left join unit u on m."Unit_id" = u.id
	                left join mat_grp mg on m."MaterialGroup_id" = mg.id
	                left join sys_code sc on mg."MaterialType" = sc."Code" 
	                and sc."CodeType" ='mat_type'
	                where 1=1
	                and m."LotUseYN" = 'Y'
				  """;
			
			if(!matType.isEmpty()) sql += "and mg.\"MaterialType\" = :matType ";
			if(matGrpId != null) sql += "and mg.\"id\" = :matGrpId ";
			if(!keyword.isEmpty()) sql += " and m.\"Name\" like concat('%%',:keyword,'%%') ";
			
			MapSqlParameterSource paramMap = new MapSqlParameterSource();
			paramMap.addValue("matType", matType);
			paramMap.addValue("matGrpId", matGrpId);
			paramMap.addValue("keyword", keyword);
			
			result.data = this.sqlRunner.getRows(sql, paramMap);
		}
		
		
		return result;
	}
	
	@RequestMapping("/pop_prod_input/mat_lot_list")
	public AjaxResult getMatLotList(
			@RequestParam(value="mat_pk", required=false) Integer matPk,
			@RequestParam(value="jr_pk", required=false) Integer jrPk) {
	
		AjaxResult result = new AjaxResult();
		
		String sql = """
		        with aa as (
		        	select mpi."MaterialLot_id" as mat_lot_id from job_res jr 
			        inner join mat_proc_input mpi on jr."MaterialProcessInputRequest_id" = mpi."MaterialProcessInputRequest_id" 
			        where jr.id = :jrPk
		        )
		       	select 
				a.id, m."Name" as mat_name, a."LotNumber" as lot_number
		        , a."CurrentStock" as cur_stock
		        , a."InputQty" as first_qty
		        , sh."Name" as storehouse_name
		        , FORMAT(a."EffectiveDate",'yyyy-mm-dd') as effective_date
		        , FORMAT(a."InputDateTime",'yyyy-mm-dd') as create_date
		        , case when aa.mat_lot_id is not null then 'Y' else 'N' end as lot_use
		        from mat_lot a
		        inner join material m on m.id = a."Material_id"
		        left join aa on aa.mat_lot_id = a.id
		        left join store_house sh on sh.id = a."StoreHouse_id" 
		        where a."Material_id" = :matPk
		        and a."CurrentStock" > 0
		        order by a."EffectiveDate" , a."InputDateTime" 
				""";
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("matPk", matPk);
		paramMap.addValue("jrPk", jrPk);
		
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;
	}
	
	@RequestMapping("/pop_prod_input/lot_info")
	public AjaxResult getLotInfo(
			@RequestParam(value="lot_number", required=false) String lotNumber) {
		
		AjaxResult result = new AjaxResult();
		
		String sql = """
			select a.id
			, mg."Name" as mat_grp_name
			, m."Name" as mat_name
	        , a."CurrentStock" as cur_stock
	        , a."InputQty" as first_qty
	        , sh."Name" as storehouse_name
		    , FORMAT(a."EffectiveDate",'yyyy-mm-dd') as effective_date
		    , FORMAT(a."InputDateTime",'yyyy-mm-dd') as create_date
		    from mat_lot a
		    inner join material m on m.id = a."Material_id" 
		    left join store_house sh on sh.id = a."StoreHouse_id"
		    left join mat_grp mg on mg.id = m."MaterialGroup_id" 
		    where a."LotNumber" = :lotNumber
			""";
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("lotNumber", lotNumber);
		
		result.data = this.sqlRunner.getRow(sql, paramMap);
		
		return result;
	}
	
	@RequestMapping("/search_approver/read")
	public List<Map<String, Object>> getSearchApprover(
			@RequestParam(value="depart_id", required=false) Integer depart_id,
			@RequestParam(value="keyword", required=false) String keyword) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("depart_id", depart_id);
		paramMap.addValue("keyword", keyword);
				
		String sql = """
				select up."User_id"
		        ,up."Name"
		        ,up."Depart_id" 
		        ,d."Name" as "DepartName"
		        from user_profile up 
		        left join depart d on d.id = up."Depart_id"
	            where 1=1 
				""";
		
		if (keyword != null) {
			sql += " and upper(up.\"Name\") like concat('%%',upper(:keyword),'%%') ";
        }
        
		if (depart_id != null) {
        	sql += " and up.\"Depart_id\" = :depart_id ";
        }
        
    	sql += " order by COALESCE(d.\"Name\",'Z') , up.\"Name\" ";
    	
		
    	List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}
	
	@RequestMapping("/search_user_code/read")
	public List<Map<String, Object>> getSearchUserCode(
			@RequestParam(value="parent_code", required=false) String parentCode){
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("parentCode", parentCode);
		
		String sql = """
	            select c.id, c."Code", c."Value", c."Description"
	            from user_code c
	            where exists (
		            select 1
		            from user_code
		            where "Code" = :parentCode
		            and "Parent_id" is null
		            and c."Parent_id" = id
	            )
	            order by _order
				""";
		
    	List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
		
	}

	@RequestMapping("/search_Comp")
	public AjaxResult getSearchComp(
			@RequestParam(value = "compCode", required = false) String compCode,
			@RequestParam(value = "compName", required = false) String compName,
			@RequestParam(value = "business_number", required = false) String business_number){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("compCode", compCode);
		paramMap.addValue("compName", compName);
		paramMap.addValue("business_number", business_number);
		AjaxResult result = new AjaxResult();

		String sql = """
            select id as id
            , "Name" as compname
            , "Code" as compcode
            , "BusinessNumber" as business_number
            , "TelNumber" as tel_number
            , "CEOName" as invoiceeceoname
            , "Address" as invoiceeaddr
            , "BusinessType" as invoiceebiztype
            , "BusinessItem" as invoiceebizclass
            , "AccountManager" as invoiceecontactname1
            , "AccountManagerPhone" as invoiceetel1
            , "Email" as invoiceeemail1
            from company
            where "relyn" = '0'
			""";

		if (compCode != null && !compCode.isEmpty()) {
			sql += " AND LOWER([Code]) LIKE LOWER(:compCode) ";
			paramMap.addValue("compCode", "%" + compCode + "%");
		}

		if (compName != null && !compName.isEmpty()) {
			sql += " AND LOWER([Name]) LIKE LOWER(:compName) ";
			paramMap.addValue("compName", "%" + compName + "%");
		}

		if (business_number != null && !business_number.isEmpty()) {
			sql += " AND [BusinessNumber] LIKE :business_number ";
			paramMap.addValue("business_number", "%" + business_number + "%");
		}

		sql += " ORDER BY [Name] ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);

		return result;
	}

	@RequestMapping("/search_model_history")
	public AjaxResult getModel_history(
			@RequestParam(value = "modeltxt" ) String modeltxt){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("modelid", modeltxt);
		AjaxResult result = new AjaxResult();

		String sql = """
      select 
				 mh.modelid,
				 mh.cltnm ,
				 STUFF(STUFF(mh.reqdate, 5, 0, '-'), 8, 0, '-') AS reqdate, 
				 mh.version_no ,
				 mh.modeltxt_current
				 from model_history mh
				 where modelid =:modelid
				 ORDER BY version_no DESC
			""";

		result.data = this.sqlRunner.getRows(sql, paramMap);

		return result;
	}



}