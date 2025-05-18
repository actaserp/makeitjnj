package mes.app.definition.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class YearamtService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    /* tb_salesment 매출 내역T
        tb_invoicement 매입 내역T
        tb_banktransit 입출금 관리T
        tb_yearamt 연마감 T
        */


    /*public boolean checkYearamtExists(String year, String ioflag, String cltid, String name) {
        String checkSql = """
        SELECT COUNT(*) FROM tb_yearamt
        WHERE yyyymm = :yyyymm AND ioflag = :ioflag
    """;

        MapSqlParameterSource checkParam = new MapSqlParameterSource();
        checkParam.addValue("yyyymm", year + "12");
        checkParam.addValue("ioflag", ioflag);
        checkParam.addValue("searchid", cltid);
        checkParam.addValue("name", name);

        int count = this.sqlRunner.queryForCount(checkSql, checkParam);
        return count > 0;
    }


    public List<Map<String, Object>> getYearamtList(String year, String ioflag, String cltid, String name) {

        if (checkYearamtExists(year, ioflag,cltid,name)) {
            // tb_yearamt에 데이터가 있을 경우 간단한 쿼리 수행
            String simpleSql = """
            SELECT 
            COALESCE(a.ioflag, :ioflag) AS ioflag,
            a.yyyymm, 
            a.cltcd, 
            COALESCE(a.yearamt, 0) AS balance,
            a.endyn AS endyn, 
            c."Name" AS company_name
            FROM tb_yearamt a
            LEFT JOIN company c ON a.cltcd = c.id
            WHERE yyyymm = :yyyymm
              AND ioflag = :ioflag
              AND cltcd::text LIKE concat('%', :searchid, '%')
              AND c."Name" LIKE concat('%', :name, '%')
            ORDER BY a.cltcd
        """;

            MapSqlParameterSource simpleParam = new MapSqlParameterSource();
            simpleParam.addValue("yyyymm", year + "12");
            simpleParam.addValue("ioflag", ioflag);
            simpleParam.addValue("searchid", cltid);
            simpleParam.addValue("name", name);

            return this.sqlRunner.getRows(simpleSql, simpleParam);

        }else{
            MapSqlParameterSource dicParam = new MapSqlParameterSource();
            dicParam.addValue("year", year);
            dicParam.addValue("ioflag", ioflag);
            dicParam.addValue("searchid", cltid);
            dicParam.addValue("name", name);

            *//*System.out.println(dicParam.getValues()); *//* // 파라미터 값 확인

            String sql = """
                    WITH all_cltcds AS (
                        SELECT cltcd FROM (
                            SELECT cltcd, 'salesment' AS src FROM tb_salesment WHERE :ioflag = '0'
                            UNION ALL
                            SELECT cltcd, 'invoicement' AS src FROM tb_invoicement WHERE :ioflag = '1'
                        ) sales_union
                        WHERE cltcd IS NOT NULL
                          AND (
                              (src = 'salesment' AND :ioflag = '0' AND EXISTS (
                                  SELECT 1 FROM tb_salesment WHERE cltcd = sales_union.cltcd AND misdate BETWEEN '20000101' AND :year || '1231'
                              ))
                              OR
                              (src = 'invoicement' AND :ioflag = '1' AND EXISTS (
                                  SELECT 1 FROM tb_invoicement WHERE cltcd = sales_union.cltcd AND misdate BETWEEN '20000101' AND :year || '1231'
                              ))
                          )
                    
                        UNION
                    
                        SELECT cltcd FROM tb_banktransit
                        WHERE trdate BETWEEN '20000101' AND :year || '1231'
                          AND cltcd IS NOT NULL
                    
                        UNION
                    
                        SELECT cltcd FROM tb_yearamt
                        WHERE yyyymm = :year || '12'
                          AND ioflag = :ioflag
                          AND endyn = 'Y'
                          AND cltcd IS NOT NULL
                    )
                    
                    SELECT
                        a.cltcd,
                        c."Name" AS company_name,
                        y.endyn AS endyn,
                        COALESCE(y.ioflag, :ioflag) AS ioflag,
                        COALESCE(y.yearamt, 0) AS yearamt,
                        COALESCE(s.total_sales, 0) AS total_sales,
                        COALESCE(b.total_income, 0) AS total_income,
                        (COALESCE(y.yearamt, 0) + COALESCE(s.total_sales, 0) - COALESCE(b.total_income, 0)) AS balance,
                        :year || '12' AS yyyymm
                    
                    FROM all_cltcds a
                   
                    LEFT JOIN (
                        SELECT cltcd, SUM(totalamt) AS total_sales
                        FROM tb_salesment
                        WHERE :ioflag = '0'
                          AND misdate BETWEEN '20000101' AND :year || '1231'
                        GROUP BY cltcd
                    
                        UNION ALL
                    
                        SELECT cltcd, SUM(totalamt) AS total_sales
                        FROM tb_invoicement
                        WHERE :ioflag = '1'
                          AND misdate BETWEEN '20000101' AND :year || '1231'
                        GROUP BY cltcd
                    ) s ON a.cltcd = s.cltcd
                    
                    LEFT JOIN (
                        SELECT cltcd, SUM(
                            CASE WHEN :ioflag = '0' THEN accin ELSE accout END
                        ) AS total_income
                        FROM tb_banktransit
                        WHERE trdate BETWEEN '20000101' AND :year || '1231'
                        GROUP BY cltcd
                    ) b ON a.cltcd = b.cltcd
                    
                    LEFT JOIN (
                        SELECT cltcd, yearamt, endyn, ioflag, yyyymm
                        FROM tb_yearamt
                        WHERE yyyymm = :year || '12'
                          AND ioflag = :ioflag
                    ) y ON a.cltcd = y.cltcd
                    
                    LEFT JOIN company c ON a.cltcd = c.id
                    WHERE a.cltcd::text LIKE concat('%', :searchid, '%')
                    AND c."Name" LIKE concat('%', :name, '%')
                    ORDER BY a.cltcd 
                    """;

            return this.sqlRunner.getRows(sql, dicParam);
        }
    }*/


    public List<Map<String, Object>> getYearamtList3(String year, String ioflag, String cltid, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("searchid", cltid);
        dicParam.addValue("name", name);

        int targetYear = Integer.parseInt(year) - 1;
        String yyyymm = targetYear + "12";
        dicParam.addValue("yyyymm", yyyymm);

        String sql= """                           
                SELECT
                    c.id,
                    c."Name" AS company_name,
                    COALESCE(y.yearamt, s.totalamt_sum - b.accin_sum) AS balance
                FROM company c
                LEFT JOIN (
                    SELECT cltcd, yearamt
                    FROM tb_yearamt
                    WHERE yyyymm = :yyyymm
                ) y ON c.id = y.cltcd
                LEFT JOIN (
                    SELECT
                        cltcd,
                        SUM(totalamt) AS totalamt_sum
                    FROM tb_salesment
                    WHERE misdate BETWEEN '20000101' AND :year || '1231'
                    GROUP BY cltcd
                ) s ON c.id = s.cltcd
                LEFT JOIN (
                    SELECT
                        cltcd,
                        SUM(accin) AS accin_sum
                    FROM tb_banktransit
                    WHERE trdate BETWEEN '20000101' AND :year || '1231'
                    GROUP BY cltcd
                ) b ON c.id = b.cltcd
                WHERE c.relyn = '0'
                 AND c.id::text LIKE concat('%', :searchid, '%')
                 AND c."Name" LIKE concat('%', :name, '%')
                ORDER BY c.id;
                """;

        return this.sqlRunner.getRows(sql, dicParam);

    }

    public List<Map<String, Object>> getYearamtList2(String year, String ioflag, String cltid, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("searchid", cltid);
        dicParam.addValue("name", name);

        int targetYear = Integer.parseInt(year) - 1;
        String yyyymm = targetYear + "12";
        dicParam.addValue("yyyymm", yyyymm);

        String sql= """                           
                SELECT
                    c.id,
                    c."Name" AS company_name,
                    COALESCE(y.yearamt + s.totalamt_sum - b.accin_sum) AS balance
                FROM company c
                LEFT JOIN (
                    SELECT cltcd, yearamt
                    FROM tb_yearamt
                    WHERE yyyymm = :yyyymm
                ) y ON c.id = y.cltcd
                LEFT JOIN (
                    SELECT
                        cltcd,
                        SUM(totalamt) AS totalamt_sum
                    FROM tb_salesment
                    WHERE misdate BETWEEN :year || '0101' AND :year || '1231'
                    GROUP BY cltcd
                ) s ON c.id = s.cltcd
                LEFT JOIN (
                    SELECT
                        cltcd,
                        SUM(accin) AS accin_sum
                    FROM tb_banktransit
                    WHERE trdate BETWEEN :year || '0101' AND :year || '1231'
                    GROUP BY cltcd
                ) b ON c.id = b.cltcd
                WHERE c.relyn = '0'
                 AND c.id::text LIKE concat('%', :searchid, '%')
                 AND c."Name" LIKE concat('%', :name, '%')
                ORDER BY c.id;
                """;

        return this.sqlRunner.getRows(sql, dicParam);

    }


    public List<Map<String, Object>> getYearamtList(String year, String ioflag, String cltid, String name) {
        // 공통 파라미터 설정
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("year", year);
        dicParam.addValue("ioflag", ioflag);
        dicParam.addValue("searchid", cltid);
        dicParam.addValue("name", name);

        int targetYear = Integer.parseInt(year) - 1;
        String yyyymm = targetYear + "12";
        dicParam.addValue("yyyymm", yyyymm);

        String sql;
            if ("0".equals(ioflag)) {
                sql = """
            SELECT
                c.id,
                c."Name" AS company_name,
                COALESCE(
                    COALESCE(y.yearamt, 0) + COALESCE(s.totalamt_sum, 0) - COALESCE(b.accin_sum, 0),
                    0
                ) AS balance,
                 COALESCE(y.ioflag, :ioflag) AS ioflag,
                :year || '12' AS yyyymm,
                COALESCE(m.endyn, 'N') AS endyn
            FROM company c
            LEFT JOIN (
                SELECT cltcd, yearamt, ioflag
                FROM tb_yearamt
                WHERE yyyymm = :yyyymm
            ) y ON c.id = y.cltcd
            LEFT JOIN (
                SELECT cltcd, endyn
                FROM tb_yearamt
                WHERE yyyymm = :year || '12'
            ) m ON c.id = m.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(totalamt) AS totalamt_sum
                FROM tb_salesment
                WHERE misdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) s ON c.id = s.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(accin) AS accin_sum
                FROM tb_banktransit
                WHERE trdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) b ON c.id = b.cltcd
            WHERE c.relyn = '0'
              AND c.id::text LIKE concat('%', :searchid, '%')
              AND c."Name" LIKE concat('%', :name, '%')
            ORDER BY c.id
        """;
            } else {
                sql = """
            SELECT
                c.id,
                c."Name" AS company_name,
                COALESCE(
                    COALESCE(y.yearamt, 0) + COALESCE(s.totalamt_sum, 0) - COALESCE(b.accout_sum, 0),
                    0
                ) AS balance,
                COALESCE(y.ioflag, :ioflag) AS ioflag,
                :year || '12' AS yyyymm,
                COALESCE(m.endyn, 'N') AS endyn
            FROM company c
            LEFT JOIN (
                SELECT cltcd, yearamt, ioflag
                FROM tb_yearamt
                WHERE yyyymm = :yyyymm
            ) y ON c.id = y.cltcd
            LEFT JOIN (
                SELECT cltcd, endyn
                FROM tb_yearamt
                WHERE yyyymm = :year || '12'
            ) m ON c.id = m.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(totalamt) AS totalamt_sum
                FROM tb_invoicement
                WHERE misdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) s ON c.id = s.cltcd
            LEFT JOIN (
                SELECT cltcd, SUM(accout) AS accout_sum
                FROM tb_banktransit
                WHERE trdate BETWEEN '20000101' AND :year || '1231'
                GROUP BY cltcd
            ) b ON c.id = b.cltcd
            WHERE c.relyn = '0'
              AND c.id::text LIKE concat('%', :searchid, '%')
              AND c."Name" LIKE concat('%', :name, '%')
            ORDER BY c.id
        """;
        }


        return namedParameterJdbcTemplate.queryForList(sql, dicParam);

    }

}
