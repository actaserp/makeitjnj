package mes.app.definition.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import mes.domain.entity.Bom;
import mes.domain.entity.BomComponent;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.BomComponentRepository;
import mes.domain.repository.BomRepository;
import mes.domain.services.CommonUtil;
import mes.domain.services.DateUtil;
import mes.domain.services.SqlRunner;

@Slf4j
@Repository
public class BomService {

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	BomRepository bomRepository;

	@Autowired
	BomComponentRepository bomComponentRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	/**
	 *
	 * @param mat_type
	 * @param mat_group
	 * @param bom_type
	 * @param mat_name
	 * @param not_past_flag
	 * @return
	 */
	public List<Map<String, Object>> getBomMaterialList(String mat_type, Integer mat_group,	String bom_type, String mat_name,String not_past_flag,String spjangcd){

		String sql = """        		
        		with A as (select b.id
				, b.Name
				, b.BOMType
				, dbo.fn_code_name('bom_type', b.BOMType) as bom_type_name
				, b.OutputAmount
				, b.Version
				, CONVERT(VARCHAR(10), b.StartDate, 120)  as StartDate
				, CONVERT(VARCHAR(10), b.EndDate, 120)   as EndDate
				, b.Material_id
				, m.Name as mat_name
				, m.Code as mat_code
				, mg.Name as mat_group_name
				, dbo.fn_code_name('mat_type', mg.MaterialType) as mat_type
				, u.Name as unit
				, row_number() over (partition by b."BOMType", b.Material_id order by b.StartDate desc) as g_idx
				, case when CONVERT(VARCHAR(10), getdate(), 120) between CONVERT(VARCHAR(10), b.StartDate, 120)  and CONVERT(VARCHAR(10), b.EndDate, 120) then 'current'
				    when b.StartDate is null or b.EndDate is null then 'error'
					when CONVERT(VARCHAR(10), getdate(), 120)    >  CONVERT(VARCHAR(10), b.EndDate, 120)    then 'past' 
					when CONVERT(VARCHAR(10), getdate(), 120)    <  CONVERT(VARCHAR(10), b.StartDate, 120)  then 'future'
					else 'error' end as current_flag
				from bom b 
				left join material m on b.Material_id = m.id 
				left join unit u on u.id = m.Unit_id
				left join mat_grp mg on mg.id=m.MaterialGroup_id 
				where 1=1
				AND b.spjangcd = :spjangcd
                """;

		if (StringUtils.hasText(mat_type)){
			sql+= """                		
                and mg.MaterialType = :mat_type
                """;
		}

		if (mat_group!=null){
			sql+="""                		
                and m.MaterialGroup_id = :mat_group
                """;
		}
		if (StringUtils.hasText(bom_type)){
			sql+="""            		
                and b.BOMType = :bom_type
                """;
		}

		if(StringUtils.hasText( mat_name))
			sql+=""" 
                and  (m.Code like concat('%%',:mat_name,'%%') or m.Name like concat('%%',:mat_name,'%%') )
                """;
		sql += """            		
            )
            select *
            from A
            """;
		if (not_past_flag.equals("Y")){
			sql += """
                where ( A.current_flag in ( 'current','future') or A.g_idx = 1 )
                """;
		}

		sql += """            		
            order by A.mat_group_name, A.mat_code , A.mat_name , A.Material_id, A.bom_type_name
            """;


		MapSqlParameterSource paramMap = new MapSqlParameterSource();

		paramMap.addValue("mat_type", mat_type);
		paramMap.addValue("mat_group", mat_group);
		paramMap.addValue("bom_type", bom_type);
		paramMap.addValue("mat_name", mat_name);
		paramMap.addValue("spjangcd", spjangcd);
//		log.info("🔍 [BOM Material List] 실행 SQL: {}", sql);
//		log.info("🔍 [BOM Material List] 파라미터: {}", paramMap.getValues());

		return this.sqlRunner.getRows(sql, paramMap);

	}

	public Bom getBom(int id) {
		return this.bomRepository.getBomById(id);
	}


	/**
	 *
	 * @param id
	 * @return
	 */
	public Map<String, Object> getBomDetail(int id){

		String sql = """				
	            select b.id
	            , b.Name
	            , b.BOMType
	            , b.OutputAmount
	            , b.Version
	            , CONVERT(VARCHAR(10), b.StartDate, 120)   as StartDate
	            , CONVERT(VARCHAR(10), b.EndDate, 120)     as EndDate
	            , b.Material_id
	            , m.Name as MaterialName
	            , m.Code as mat_code
	            , mg.Name as mat_group_name
	            , dbo.fn_code_name('mat_type', mg.MaterialType) as mat_type
	            from bom b 
	            left join material m on b.Material_id = m.id 
	            left join mat_grp mg on mg.id = m.MaterialGroup_id
	            where b.id=:id				
	        """;

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", id);
		return this.sqlRunner.getRow(sql, paramMap);
	}

	/**
	 *
	 * @param id
	 * @param materialId
	 * @param bomType
	 * @param version
	 * @return
	 */
	public boolean checkSameVersion(Integer id, Integer materialId, String bomType, String version) {
		boolean result = true;

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("Material_id", materialId);
		paramMap.addValue("BOMType", bomType);
		paramMap.addValue("Version", version);

		String sql ="select 1 from bom where \"Material_id\"=:Material_id and \"BOMType\"=:BOMType and \"Version\"=:Version";

		if (id!=null) {
			paramMap.addValue("id", id);
			sql+=" and id!=:id";
		}

		List<Map<String, Object>> mapList = this.sqlRunner.getRows(sql, paramMap);
		if(mapList==null || mapList.size() == 0) {
			result = false;
		}

		return result;
	}


	/**
	 *
	 * @param id
	 * @param materialId
	 * @param bomType
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public boolean checkDuplicatePeriod(Integer id, Integer materialId, String bomType, String startDate, String endDate) {

		boolean result = true;

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("materialId", materialId, java.sql.Types.INTEGER);
		paramMap.addValue("bomType", bomType);
		paramMap.addValue("startDate", startDate, java.sql.Types.TIMESTAMP);
		paramMap.addValue("endDate", endDate, java.sql.Types.TIMESTAMP);

		String sql = """
        select count(*) as cnt 
        from bom 
        where "Material_id" = :materialId 
          and "BOMType" = :bomType  
          and "StartDate" <= :endDate 
          and "EndDate" >= :startDate
    """;

		if (id != null) {
			paramMap.addValue("bom_id", id);
			sql += " and id <> :bom_id";
		}

		log.info("🔍 [중복 기간 체크] 실행 SQL: {}", sql);
		log.info("🔍 [중복 기간 체크] 파라미터: {}", paramMap.getValues());

		List<Map<String, Object>> mapList = this.sqlRunner.getRows(sql, paramMap);

		if (mapList == null || ((Number) mapList.get(0).get("cnt")).longValue() == 0L) {
			result = false;
		}

		return result;
	}


	public Bom saveBom(Bom bom){
		return this.bomRepository.save(bom);
	}

	public BomComponent saveBomComponent(BomComponent bomComp) {
		return this.bomComponentRepository.save(bomComp);
	}

	public BomComponent getBomComponent(int bcid) {
		return this.bomComponentRepository.getBomComponentById(bcid);
	}

	public Map<String, Object> getBomComponentDetail(int bcid){

		String sql = """
            select bc.id
              , bc.BOM_id
              , dbo.fn_code_name('mat_type', mg."MaterialType") as mat_type
              , mg.Name as group_name
              , m.Name as "MaterialName"
              , m.Code as mat_code
              , bc.Amount
              , bc.Material_id
              , m.Unit_id
              , u.Name as unit
              , bc.Description
              , bc._order
              , bom.Name as bom_name
              , pm.Name as ParentMaterialName
              , bom.Material_id as ParentMaterial_id
            from bom_comp bc
            inner join bom on bom.id=bc.BOM_id
            left join material m on bc.Material_id=m.id
            left join material pm on bom.Material_id=pm.id
            left join unit u on u.id = m.Unit_id 
            left join mat_grp mg on m.MaterialGroup_id =mg.id
            where bc.id = :id				
		""";

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", bcid);

		return this.sqlRunner.getRow(sql, paramMap);
	}

	public int deleteBomComponent(int bc_id) {
		int iRowEffected = 0;
		String sql ="""
		DELETE FROM bom_comp WHERE id=:bc_id				
		""";
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("bc_id", bc_id);
		iRowEffected=this.sqlRunner.execute(sql, paramMap);
		return iRowEffected;
	}


	public List<Map<String, Object>> getBomComponentTreeList(int bomId){

		String sql = """
 WITH bom_tree AS (
				    -- Anchor part
				    SELECT
				        1 AS lvl,
				        bc.Material_id,
				        CAST(bc._order AS INT) AS item_order,
				        bc.Material_id AS parent_mat_id,
				        bc.Amount AS quantity,
				        b1.produced_qty,
				        CAST(bc.Amount / NULLIF(b1.produced_qty, 0) AS FLOAT) AS bom_ratio,
				        bc.Description,
				        CAST('base' AS VARCHAR(10)) AS data_div ,
				        bc.id AS bc_id,
				        CAST(RIGHT(REPLICATE('0', 4) + CAST(bc._order AS VARCHAR), 4) AS VARCHAR(100)) AS tot_order,
				        CAST(bc.Material_id AS VARCHAR(100)) AS my_key,
				        CAST('' AS VARCHAR(100)) AS parent_key
				    FROM bom_comp bc
				    INNER JOIN (
				        SELECT
				            b1.id AS bom_pk,
				            b1.Material_id AS prod_pk,
				            NULLIF(b1.OutputAmount, 0) AS produced_qty,
				            ROW_NUMBER() OVER (PARTITION BY b1.Material_id ORDER BY b1.StartDate DESC) AS g_idx
				        FROM bom b1
				        INNER JOIN bom b ON b1.BOMType = b.BOMType
				        WHERE b.id = :id
				    ) b1 ON b1.bom_pk = bc.BOM_id
				    WHERE bc.BOM_id = :id
				    UNION ALL
				    -- Recursive part
				    SELECT
				        bt.lvl + 1,
				        bc.Material_id,
				        CAST(bc._order AS INT),
				        bt.Material_id AS parent_mat_id,
				        bc.Amount,
				        b1.produced_qty,
				        CAST(bc.Amount / NULLIF(b1.produced_qty, 0) * bt.bom_ratio AS FLOAT),
				        bc.Description,
				        CAST('child' AS VARCHAR(10)) AS data_div,
				        bc.id,
				        CAST(
						    CAST(bt.tot_order AS VARCHAR(100)) + '-' +
						    RIGHT(REPLICATE('0', 4) + CAST(bc._order AS VARCHAR), 4)
						    AS VARCHAR(100)) AS tot_order,
				        CAST(bt.my_key + '-' + CAST(bc.Material_id AS VARCHAR(100)) AS VARCHAR(100)) AS my_key,
				        bt.my_key
				    FROM bom_tree bt
				    INNER JOIN (
				        SELECT
				            b1.id AS bom_pk,
				            b1.Material_id AS prod_pk,
				            NULLIF(b1.OutputAmount, 0) AS produced_qty,
				            ROW_NUMBER() OVER (PARTITION BY b1.Material_id ORDER BY b1.StartDate DESC) AS g_idx
				        FROM bom b1
				        INNER JOIN bom b ON b1.BOMType = b.BOMType
				        WHERE b.id = :id
				    ) b1 ON b1.prod_pk = bt.Material_id AND b1.g_idx = 1
				    INNER JOIN bom_comp bc ON bc.BOM_id = b1.bom_pk
				)
				-- Final result
				SELECT
				    bt.lvl,
				    bt.my_key,
				    CASE WHEN bt.data_div = 'child' THEN bt.parent_key END AS parent_key,
				    bt.Material_id AS mat_id,
				    CASE WHEN bt.data_div = 'child' THEN bt.parent_mat_id END AS parent_mat_id,
				    dbo.fn_code_name('mat_type', mg.MaterialType) AS mat_type,
				    m.Name AS mat_name,
				    m.Code AS mat_code,
				    bt.quantity,
				    bt.produced_qty,
				    CAST(bt.bom_ratio AS NUMERIC(15,7)) AS bom_ratio,
				    CAST(bt.quantity AS VARCHAR(20)) + '/' + CAST(bt.produced_qty AS VARCHAR(20)) AS bom_qty,
				    u.Name AS unit,
				    bt.Description,
				    bt.bc_id,
				    bt.tot_order,
				    CASE 
								 WHEN bt.lvl = 1 THEN '모품목'
								 ELSE '자품목'
						 END AS part_type
				FROM bom_tree bt
				INNER JOIN material m ON m.id = bt.Material_id
				LEFT JOIN unit u ON u.id = m.Unit_id
				LEFT JOIN mat_grp mg ON m.MaterialGroup_id = mg.id
				ORDER BY bt.tot_order ASC						
		""";

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", bomId);
		//log.info("getBomComponentTreeList: {} ", sql);
		//log.info(" [getBomComponentTreeList] 파라미터: {}", paramMap.getValues());
		return this.sqlRunner.getRows(sql, paramMap);
	}

	public boolean checkDuplicateBomComponent(int bomId, Integer materialId) {
		boolean exist = false;

		String sql = """
		select count(*) from bom_comp where Material_id=:materialId and BOM_id=:bomId	
		""";

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("materialId", materialId);
		paramMap.addValue("bomId", bomId);

		int count = this.sqlRunner.queryForCount(sql, paramMap);
		exist = count==0?false:true;
		return exist;
	}

	public AjaxResult bomReplicate(int bomId, User user) {

		Bom bom = this.bomRepository.getBomById(bomId);

		int materialId = bom.getMaterialId();
		//new_bom.StartDate = '1900-01-01'
		//new_bom.EndDate = '1900-01-01'

		String sql = """
		select count(*) from bom where Material_id=:materialId and StartDate='1900-01-01' or EndDate='1900-01-01'
        """;
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("materialId", materialId);
		int count = this.sqlRunner.queryForCount(sql, paramMap);

		AjaxResult result = new AjaxResult();
		if (count>0) {
			result.success = false;
			result.message = "복제된 BOM중 수정되지 않은 BOM이 \\n 존재하여 복제를 수행할 수 없습니다.";
			return result;
		}

		Float fVer=CommonUtil.tryFloat(bom.getVersion()) + (float)0.1;
		String newVer = fVer.toString();
		String newName = String.format("%s_Copy", bom.getName());

		this.transactionTemplate.executeWithoutResult(status->{

			try {
				Bom newBom = new Bom();
				newBom.setName(newName);
				newBom.setVersion(newVer);
				newBom.setMaterialId(materialId);
				newBom.setBomType(bom.getBomType());
				newBom.setOutputAmount(bom.getOutputAmount());
				newBom.setStartDate(Timestamp.valueOf("1900-01-01 00:00:00"));
				newBom.setEndDate(Timestamp.valueOf("1900-01-01 00:00:00"));
				newBom.set_audit(user);
				//신규BOM저장
				this.bomRepository.save(newBom);

				//bom component 저장=>기존 component를 가져와서 저장
				String sqlInsert = """
		        insert into bom_comp(BOM_id, Material_id , Amount , _order , Description , _created , _creater_id )
			    select :new_pk as bom_pk, Material_id , Amount , _order , Description , now() , :user_pk
			    from bom_comp bc 
			    where BOM_id = :bom_pk				
				""";
				MapSqlParameterSource insertMap = new MapSqlParameterSource();
				insertMap.addValue("new_pk", newBom.getId());
				insertMap.addValue("user_pk", user.getId());
				insertMap.addValue("bom_pk", bomId);
				result.data =sqlRunner.execute(sqlInsert, insertMap);

			}
			catch(Exception ex) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				result.success=false;
				result.message = ex.toString();
			}
		});


		return result;
	}

	/**
	 *
	 * @param user
	 * @return
	 */
	public AjaxResult bomRevision(int bomId, User user) {

		AjaxResult result = new AjaxResult();
		Bom bom = this.bomRepository.getBomById(bomId);
		bom.setEndDate(DateUtil.getYesterdayTimestamp());
		bom.set_audit(user);

		this.transactionTemplate.executeWithoutResult(status->{

			try {
				this.bomRepository.save(bom);

				Float fVer=CommonUtil.tryFloat(bom.getVersion()) + 1;
				String newVer = fVer.toString();
				String newName = String.format("%s V%s", bom.getName(), newVer);

				Bom newBom = new Bom();

				newBom.setName(newName);
				newBom.setMaterialId(bom.getMaterialId());
				newBom.setBomType(bom.getBomType());
				newBom.setOutputAmount(bom.getOutputAmount());
				newBom.setVersion(newVer);

				Timestamp start = DateUtil.getNowTimeStamp();
				newBom.setStartDate(start);
				newBom.setEndDate(Timestamp.valueOf("2100-12-31 29:59:59"));
				newBom.set_audit(user);

				this.bomRepository.save(newBom);

				//bom component 저장=>기존 component를 가져와서 저장
				String sql = """
		        insert into bom_comp(BOM_id, Material_id , Amount , _order , Description , _created , _creater_id )
			    select :new_pk as bom_pk, Material_id , Amount , _order , Description , now() , :user_pk
			    from bom_comp bc 
			    where BOM_id = :bom_pk				
				""";
				MapSqlParameterSource insertMap = new MapSqlParameterSource();
				insertMap.addValue("new_pk", newBom.getId());
				insertMap.addValue("user_pk", user.getId());
				insertMap.addValue("bom_pk", bomId);
				result.data =sqlRunner.execute(sql, insertMap);

			}catch(Exception ex) {
				TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				result.success=false;
				result.message = ex.toString();
			}
		});


		return result;
	}
}
