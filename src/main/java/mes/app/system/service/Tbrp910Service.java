package mes.app.system.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class Tbrp910Service {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getCodeList(String txtCode) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("txtCode", txtCode);

        String sql = """
                select A.itemcd as id,
                       B."itemnm" as groupnm,
                       A.groupcd,
                       A.itemcd,
                       A.itemnm,
                       A.itemrm
                from TB_RP910 A
                LEFT OUTER JOIN TB_RP910 B on B.itemcd = A.groupcd
                where A."itemnm" like concat('%',:txtCode,'%')
                """;
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;

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
            if (typeClassTable.equals("user_code")) {
                funcName = "fn_user_code_name";
            }
        }

        if (type2ClassTable != null) {
            if (type2ClassTable.equals("user_code")) {
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
        if (tableName2.equals("master_t")) {
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

}