package mes.app.definition.service.material;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.thymeleaf.util.MapUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UnitPriceService {

	@Autowired
	SqlRunner sqlRunner;
	
	public List<Map<String, Object>> getPriceListByMat(int matPk){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("mat_pk", matPk);
        
        String sql = """
						WITH A AS (
						    SELECT\s
						        mcu.id,
						        mcu.Company_id,
						        mcu.UnitPrice,
						        mcu.FormerUnitPrice,
						        mcu.ApplyStartDate,
						        mcu.ApplyEndDate,
						        mcu.ChangeDate,
						        mcu.Material_id,
						        ROW_NUMBER() OVER (PARTITION BY mcu.Company_id ORDER BY mcu.ApplyStartDate DESC) AS g_idx,
						        CASE WHEN GETDATE() BETWEEN mcu.ApplyStartDate AND mcu.ApplyEndDate THEN 1 ELSE 0 END AS current_check,
						        CASE WHEN GETDATE() < mcu.ApplyStartDate THEN 1 ELSE 0 END AS future_check
						    FROM mat_comp_uprice mcu
						    WHERE mcu.Material_id = :mat_pk
						)
						SELECT\s
						    A.id,
						    A.Company_id,
						    c.Name AS CompanyName,
						    A.UnitPrice,
						    A.FormerUnitPrice,
						    CAST(A.ApplyStartDate AS DATE) AS ApplyStartDate,
						    CAST(A.ApplyEndDate AS DATE) AS ApplyEndDate,
						    CAST(A.ChangeDate AS DATE) AS ChangeDate,
						    A.Material_id
						FROM A
						INNER JOIN company c ON c.id = A.Company_id
						WHERE\s
						    A.current_check = 1 OR A.future_check = 1 OR A.g_idx = 1
						ORDER BY c.Name, A.ApplyStartDate
        """;
        	
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		log.info("품목정보-단가정보 read SQL: {}", sql);
		log.info("SQL Parameters: {}", dicParam.getValues());
        return items;
	}
	
	public List<Map<String, Object>> getPriceHistoryByMat(int matPk){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("mat_pk", matPk);
				//dicParam.addValue("com_pk", comPk);
        
        String sql = """
						select 
						mu.idxkey  as price_id,
						 c.Name as company_name,
						 mu.PUAMT as unit_price,
						 mu.cltcd,
						CONVERT(VARCHAR(10), CONVERT(DATE, CONVERT(VARCHAR, mu.INPUTDATE)), 120) AS input_date
						 from mat_uamt mu
						 left join company c on c.id =mu.CLTCD
						 where mu.PCODE =:mat_pk
        """;
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
	}
	
	public Map<String, Object> getPriceDetail(int pricePk){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("price_pk", pricePk);
        
        String sql = """
			select mcu.id as price_id
            , m."MaterialGroup_id"
            , mcu."Material_id" 
            , mcu."Company_id" 
            , mcu."UnitPrice"
            , "FormerUnitPrice"
            , mcu."ApplyStartDate" as "ApplyStartDate"
            , mcu."ApplyEndDate" as "ApplyEndDate"
            , mcu."Type" as type
            from mat_comp_uprice mcu 
            inner join material m on m.id = mcu."Material_id" 
            where 1 = 1
            and mcu.id = :price_pk
        """;
        	
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        return item;
	}

	public int saveCompanyUnitPrice(MultiValueMap<String, Object> data) {
		Integer materialId = CommonUtil.tryIntNull(data.getFirst("Material_id"));
		Integer companyId = CommonUtil.tryIntNull(data.getFirst("Company_id"));

		/*// applyStartDate가 '2025-04-15T13:34'와 같은 형식으로 들어올 때 처리
		String applyStartDateStr = CommonUtil.tryString(data.getFirst("ApplyStartDate"));
		LocalDateTime applyStartDateLocal = LocalDateTime.parse(applyStartDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		Timestamp applyStartDate = Timestamp.valueOf(applyStartDateLocal);*/
		// ApplyStartDate 처리
		String applyStartDateStr = CommonUtil.tryString(data.getFirst("ApplyStartDate"));
		LocalDateTime applyStartDateLocal = LocalDateTime.parse(applyStartDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		Timestamp applyStartDate = Timestamp.valueOf(applyStartDateLocal);

		// 현재 날짜와 비교하여 ApplyEndDate 설정
		LocalDate applyStartDateDate = applyStartDateLocal.toLocalDate();
		LocalDate today = LocalDate.now();

		Timestamp applyEndDate;
		if (!applyStartDateDate.equals(today)) {
			// 날짜가 다르면 하루 전 날짜로 설정 (시간은 00:00:00)
			applyEndDate = Timestamp.valueOf(applyStartDateDate.minusDays(1).atStartOfDay());
		} else {
			// 날짜가 같으면 ApplyStartDate 그대로 사용
			applyEndDate = applyStartDate;
		}

		// applyEndDate는 기존대로 설정
		Timestamp applyEndDate2 = CommonUtil.tryTimestamp("2100-12-31");


		Float unitPrice = CommonUtil.tryFloatNull(data.getFirst("UnitPrice"));
		String changerName = CommonUtil.tryString(data.getFirst("ChangerName"));
		String type = CommonUtil.tryString(data.getFirst("type"));
		Integer userId = CommonUtil.tryIntNull(data.getFirst("user_id").toString());

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("materialId", materialId);
		dicParam.addValue("companyId", companyId);
		dicParam.addValue("applyStartDate", applyStartDate, java.sql.Types.TIMESTAMP);
		dicParam.addValue("applyEndDate", applyEndDate, java.sql.Types.TIMESTAMP);

		dicParam.addValue("applyEndDate2", applyEndDate2, java.sql.Types.TIMESTAMP);

		dicParam.addValue("unitPrice", unitPrice);
		dicParam.addValue("changerName", changerName);
		dicParam.addValue("userId", userId);
		dicParam.addValue("type", type);
		dicParam.addValue("formerUnitPrice", null);

		String sql = """
            select id, "UnitPrice"
            from mat_comp_uprice
            where "Material_id" = :materialId
            and "Company_id" = :companyId
            and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
            """;

		Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);

		if(!MapUtils.isEmpty(item)) {
			dicParam.addValue("formerUnitPrice", CommonUtil.tryFloatNull(item.get("UnitPrice")));
		}

		sql = """
        update mat_comp_uprice
        set "ApplyEndDate" = :applyEndDate
        where "Material_id" = :materialId
        and "Company_id" = :companyId
        and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
        """;

		this.sqlRunner.execute(sql, dicParam);

		sql = """
            INSERT INTO public.mat_comp_uprice
            ("_created"
            , "_creater_id"
            , "Material_id"
            , "Company_id"
            , "ApplyStartDate"
            , "ApplyEndDate"
            , "UnitPrice"
            , "FormerUnitPrice"
            , "ChangeDate"
            , "ChangerName"
            , "Type")
            VALUES(
            now()
            , :userId
            , :materialId 
            , :companyId
            , :applyStartDate
            , :applyEndDate2
            , :unitPrice
            , :formerUnitPrice
            , now()
            , :changerName 
            , :type)
            """;
		return this.sqlRunner.execute(sql, dicParam);
	}
	
	public int updateCompanyUnitPrice(MultiValueMap<String, Object> data){
		Integer priceId = CommonUtil.tryIntNull(data.getFirst("price_id"));
		Timestamp applyStartDate = CommonUtil.tryTimestamp(data.getFirst("ApplyStartDate"));
		Float unitPrice = CommonUtil.tryFloatNull(data.getFirst("UnitPrice"));
		String changerName = CommonUtil.tryString(data.getFirst("ChangerName"));
		Integer userId = CommonUtil.tryIntNull(data.getFirst("user_id").toString());
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("priceId", priceId);
		dicParam.addValue("applyStartDate", applyStartDate, java.sql.Types.TIMESTAMP);
		dicParam.addValue("unitPrice", unitPrice);
		dicParam.addValue("changerName", changerName);
		dicParam.addValue("userId", userId);

		String sql = """
			update mat_comp_uprice
			set "FormerUnitPrice" = "UnitPrice"
			, "UnitPrice" = :unitPrice
			, "ApplyStartDate" = :applyStartDate
			, "ChangeDate" = now()
			, "ChangerName" = :changerName
			where id = :priceId
        """;


		return this.sqlRunner.execute(sql, dicParam);
	}
	
	public int deleteCompanyUnitPrice(int priceId){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
		dicParam.addValue("priceId", priceId);
        
        String sql = """
				select id, "Material_id", "Company_id", FORMAT("ApplyStartDate",'yyyy-mm-dd') as "ApplyStartDate"
	            from mat_comp_uprice
	            where id = :priceId
				""";
		
		Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
		
    	sql = " delete from mat_comp_uprice where id = :priceId";
    	this.sqlRunner.execute(sql, dicParam);
    	
    	dicParam.addValue("materialId", CommonUtil.tryIntNull(item.get("Material_id")));
    	dicParam.addValue("companyId", CommonUtil.tryIntNull(item.get("Company_id")));
    	dicParam.addValue("applyStartDate", CommonUtil.tryTimestamp(item.get("ApplyStartDate")), java.sql.Types.TIMESTAMP);
    	
    	sql = """
    			update mat_comp_uprice
	            set "ApplyEndDate" = '2100-12-31'
	            where "Material_id" = :materialId
	            and "Company_id" = :companyId
	            and "ApplyEndDate" = (:applyStartDate)::timestamp - interval '1 days'
    			""";
    	
    	return this.sqlRunner.execute(sql, dicParam);
	}
}
