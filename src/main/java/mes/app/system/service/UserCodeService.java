package mes.app.system.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mes.domain.entity.UserCode;
import mes.domain.repository.TB_RP980Repository;
import mes.domain.repository.UserCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import mes.domain.services.SqlRunner;

@Service
public class UserCodeService {

	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserCodeRepository codeRepository;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private TB_RP980Repository tbRp980Repository;

	public List<Map<String, Object>> getCodeList(String txtCode) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("txtCode", txtCode);

		String sql = """
        WITH AGroupTB AS (
            SELECT *
            FROM user_code A
            WHERE A.[Parent_id] IS NULL
        ),
        BGroupTB AS (
            SELECT A.*
            FROM user_code A
            JOIN AGroupTB B ON A.[Parent_id] = B.id
        ),
        CGroupTB AS (
            SELECT A.*
            FROM user_code A
            JOIN BGroupTB B ON A.[Parent_id] = B.id
        )

        SELECT A.id AS aid,
               A.[Code] AS agropcd,
               A.[Value] AS agroupnm,
               A.[_status] AS astatus,
               A.[Description] AS adescription,
               B.id AS bid,
               B.[Code] AS bgropcd,
               B.[Value] AS bgroupnm,
               B.[_status] AS bstatus,
               B.[Description] AS bdescription,
               C.id AS cid,
               C.[Code] AS cgropcd,
               C.[Value] AS cgroupnm,
               C.[_status] AS cstatus,
               C.[Description] AS cdescription
        FROM AGroupTB A
        LEFT JOIN BGroupTB B ON A.id = B.[Parent_id]
        LEFT JOIN CGroupTB C ON B.id = C.[Parent_id]
        WHERE (:txtCode IS NULL OR LOWER(A.[Value]) LIKE '%' + LOWER(:txtCode) + '%')
        ORDER BY A.id, A.[Code], B.[Code], C.[Code];
        """;

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		return items;
	}

	public boolean existsByCode2(String code) {
		return codeRepository.existsByCode(code);
	}

	public boolean existsByCode(String code) {
		return codeRepository.countByCode(code) > 0;
	}

	public Map<String, Object> getCode(int id) {
		String sql = """
				select C.id
            , C."Parent_id" as parent_id
            , P."Value" as parent_name
            , C."Code" as code
            , C."Value" as name
            , C."Description" as description
            from user_code C
            left join user_code P on P.id = C."Parent_id"
            where C.id = :id
			""";

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("id", id);

		Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
		return item;
	}


	public List<Map<String, Object>> getUserCode(String parentCode, String baseDate, String type, String typeClassCode, String type2ClassCode, String typeClassTable, String type2ClassTable) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("parentCode", parentCode);
		paramMap.addValue("baseDate", baseDate);
		paramMap.addValue("type", type);
		paramMap.addValue("typeClassCode", typeClassCode);
		paramMap.addValue("type2ClassCode", type2ClassCode);
		paramMap.addValue("typeClassTable", typeClassTable);
		paramMap.addValue("type2ClassTable", type2ClassTable);

		String funcName = "dbo.fn_code_name";
		String funcName2 = "dbo.fn_code_name";

		if (typeClassTable != null) {
			if(typeClassTable.equals("user_code")) {
				funcName = "fn_user_code_name";
			}
		}

		if (type2ClassTable != null) {
			if(type2ClassTable.equals("user_code")) {
				funcName2 = "fn_user_code_name";
			}
		}


		String sql = """
				select c.id
	            , c."Parent_id" as parent_id, c."Code" as code
	            , c."Value" as name
	            , c."Type" as code_type
	            , c."Type2" as code_type2
	            , c."Description" as description
	            , c."StartDate" as start_date
	            , c."EndDate" as end_date
				""";
		if (typeClassCode != null) {
			sql += "," + funcName + "(:typeClassCode, c.\"Type\") as code_type_name ";
		}

		if (type2ClassCode != null) {
			sql += "," + funcName2 + "(:type2ClassCode, c.\"Type2\") as code_type_name2 ";
		}
		sql += """
			from user_code c
            inner join user_code pc on pc.id = c."Parent_id"
            where pc."Code" = :parentCode
            and cast(:baseDate as date) between coalesce(c."StartDate",'2000-01-01') and coalesce(c."EndDate",'2100-12-31')
			""";

		if (type != null) {
			sql += " and c.\"Type\" = :type ";
		}

		sql += " order by c.\"Type\", c._order ";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

        return items;
	}

	public Map<String, Object> userCodeDetail(Integer id) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("id", id);

		String sql = """
				select c.id
                , c."Parent_id"
                , c."Code"
                , c."Value"
                , c."Type"
                , c."Type2"
                , c."Description"
                , c."StartDate"
                , c."EndDate"
		            from user_code C
		            where C.id = :id
				""";

        Map<String, Object> items = this.sqlRunner.getRow(sql, paramMap);

        return items;
	}

	public List<Map<String, Object>> relationDataList(String tableName2, String relationName, String baseId) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("tableName2", tableName2);
		paramMap.addValue("relationName", relationName);
		paramMap.addValue("baseId", baseId);

		String sql = "";
		if(tableName2.equals("master_t")) {
			sql = """
				select rd.id, c.id as data_pk2
                , c."Code" as code, c."Name" as name
                , rd._order
                , rd."StartDate" as start_date, rd."EndDate" as end_date 
	            from rela_data rd 
	            inner join master_t c on c.id = rd."DataPk2"
	            and rd."TableName2" = 'master_t'
	            where "DataPk1" = cast(:baseId as Integer)
	            and rd."TableName1" = 'user_code'
	            and rd."RelationName" = :relationName
	            order by rd._order, rd.id 
				""";
		} else if (tableName2.equals("user_code"))
			sql = """
				select rd.id, c.id as data_pk2
                , c."Code" as code, c."Value" as name
                , rd._order
                , rd."StartDate" as start_date, rd."EndDate" as end_date 
	            from rela_data rd 
	            inner join user_code c on c.id = rd."DataPk2"
	            and rd."TableName2" = 'user_code'
	            where "DataPk1" = cast(:baseId as Integer)
	            and rd."TableName1" = 'user_code'
	            and rd."RelationName" = :relationName
	            order by rd._order, rd.id 
			     """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

        return items;
	}

	// 비상연락망 지역, 산단 목록 반환
	public Map<String, Map<String, Object>> getRegionsWithDistricts() {
		// 데이터베이스에서 지역과 산단 정보를 가져옴
		String sql = """
    WITH AGroupTB AS (
        SELECT *
        FROM user_code A
        WHERE A."Code" = 'region'
    ),
    BGroupTB AS (
        SELECT A.*, A."Value" AS bgroupnm, A."Code" AS bgroupcd
        FROM user_code A
        JOIN AGroupTB B ON A."Parent_id" = B.id
    ),
    CGroupTB AS (
        SELECT A.*, A."Value" AS cgroupnm, A."Code" AS cgroupcd
        FROM user_code A
        JOIN BGroupTB B ON A."Parent_id" = B.id
    )
    SELECT B.bgroupnm AS regionName, B.bgroupcd AS regionCode, C.cgroupnm AS districtName, C.cgroupcd AS districtCode
    FROM BGroupTB B
    LEFT JOIN CGroupTB C ON B.id = C."Parent_id"
    ORDER BY B.bgroupcd, C.cgroupcd
    """;

		List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(sql, new HashMap<>());

		// 결과를 저장할 Map
		Map<String, Map<String, Object>> regionsWithDistricts = new HashMap<>();

		for (Map<String, Object> row : results) {
			String regionName = (String) row.get("regionName");
			String regionCode = (String) row.get("regionCode");
			String districtName = (String) row.get("districtName");
			String districtCode = (String) row.get("districtCode");

			// 지역 코드와 이름을 포함하는 Map
			if (!regionsWithDistricts.containsKey(regionCode)) {
				Map<String, Object> regionData = new HashMap<>();
				regionData.put("regionName", regionName);
				regionData.put("districts", new ArrayList<Map<String, String>>());
				regionsWithDistricts.put(regionCode, regionData);
			}

			// 산단 데이터 추가
			if (districtName != null && districtCode != null) {
				Map<String, String> districtData = new HashMap<>();
				districtData.put("districtName", districtName);
				districtData.put("districtCode", districtCode);
				((List<Map<String, String>>) regionsWithDistricts.get(regionCode).get("districts")).add(districtData);
			}
		}

		return regionsWithDistricts;
	}




}