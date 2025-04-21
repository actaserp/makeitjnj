package mes.app.dashboard.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashBoardService2 {
    @Autowired
    SqlRunner sqlRunner;

    // 사용자의 사업장코드 return
    public String getSpjangcd(String username
                            , String searchSpjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT spjangcd
                FROM auth_user
                WHERE username = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> spjangcdMap = this.sqlRunner.getRow(sql, dicParam);
        String userSpjangcd = (String)spjangcdMap.get("spjangcd");

        String spjangcd = searchSpjangcd(searchSpjangcd, userSpjangcd);
        return spjangcd;
    }
    // init에 필요한 사업장코드 반환
    public String searchSpjangcd(String searchSpjangcd, String userSpjangcd){

        String resultSpjangcd = "";
        switch (searchSpjangcd){
            case "ZZ":
                resultSpjangcd = searchSpjangcd;
                break;
                case "PP":
                    resultSpjangcd= searchSpjangcd;
                    break;
                    default:
                        resultSpjangcd = userSpjangcd;
        }
        return resultSpjangcd;
    }

    // username으로 cltcd, cltnm, saupnum, custcd 가지고 오기
    public Map<String, Object> getUserInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select custcd,
                        cltcd,
                        cltnm
                FROM TB_XCLIENT
                WHERE saupnum = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    // 작년 진행구분(ordflag)별 데이터 개수
    public List<Map<String, Object>> LastYearCnt(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
            WITH DateRanges AS (
               SELECT
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + '0101' AS PrevYearStart, -- 전년도 1월 1일
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + RIGHT(CONVERT(VARCHAR(8), GETDATE(), 112), 4) AS PrevYearEnd, -- 전년도 오늘 날짜
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + '0101' AS ThisYearStart, -- 올해 1월 1일
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + RIGHT(CONVERT(VARCHAR(8), GETDATE(), 112), 4) AS ThisYearEnd, -- 올해 오늘 날짜
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + LEFT(CONVERT(VARCHAR(8), GETDATE(), 112), 6) + '01' AS ThisMonthStart, -- 올해 당월 1일
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + LEFT(CONVERT(VARCHAR(8), GETDATE(), 112), 6) + '01' AS LastYearThisMonthStart -- 작년 당월 1일
            )
            SELECT
                   ordflag,
                   COUNT(*) AS TotalCount
               FROM TB_DA006W
               CROSS JOIN DateRanges
               WHERE
                   LEN(reqdate) = 8 AND                        -- 8자리 문자열인지 확인
                   reqdate LIKE '[0-9][0-9][0-9][0-9][0-1][0-9][0-3][0-9]' AND -- YYYYMMDD 형식인지 확인
                   CONVERT(DATE, reqdate, 112) BETWEEN CONVERT(DATE, PrevYearStart, 112) AND CONVERT(DATE, PrevYearEnd, 112)
                   AND spjangcd = :spjangcd
               GROUP BY ordflag
            """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 올해 진행구분(ordflag)별 데이터 개수
    public List<Map<String, Object>> ThisYearCnt(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
            WITH DateRanges AS (
               SELECT
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + '0101' AS PrevYearStart, -- 전년도 1월 1일
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + RIGHT(CONVERT(VARCHAR(8), GETDATE(), 112), 4) AS PrevYearEnd, -- 전년도 오늘 날짜
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + '0101' AS ThisYearStart, -- 올해 1월 1일
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + RIGHT(CONVERT(VARCHAR(8), GETDATE(), 112), 4) AS ThisYearEnd, -- 올해 오늘 날짜
                   CAST(YEAR(GETDATE()) AS CHAR(4)) + LEFT(CONVERT(VARCHAR(8), GETDATE(), 112), 6) + '01' AS ThisMonthStart, -- 올해 당월 1일
                   CAST(YEAR(GETDATE()) - 1 AS CHAR(4)) + LEFT(CONVERT(VARCHAR(8), GETDATE(), 112), 6) + '01' AS LastYearThisMonthStart -- 작년 당월 1일
            )
            SELECT
                   ordflag,
                   COUNT(*) AS TotalCount
               FROM TB_DA006W
               CROSS JOIN DateRanges
               WHERE
                   LEN(reqdate) = 8 AND                        -- 8자리 문자열인지 확인
                   reqdate LIKE '[0-9][0-9][0-9][0-9][0-1][0-9][0-3][0-9]' AND -- YYYYMMDD 형식인지 확인
                   CONVERT(DATE, reqdate, 112) BETWEEN CONVERT(DATE, ThisYearStart, 112) AND CONVERT(DATE, ThisYearEnd, 112)
                   AND spjangcd = :spjangcd
               GROUP BY ordflag
            """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 올해 이번달 일별 상태값별 데이터
    public List<Map<String, Object>> ThisMonthCntOfOrdflag(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
                WITH DateSeries AS (
                         SELECT CONVERT(VARCHAR(8), DATEADD(DAY, number, CAST(DATEADD(DAY, 1 - DAY(GETDATE()), GETDATE()) AS DATE)), 112) AS reqdate
                         FROM master.dbo.spt_values
                         WHERE type = 'P' AND number BETWEEN 0 AND DAY(EOMONTH(GETDATE())) - 1
                     )
                     SELECT
                         ds.reqdate,
                         COUNT(CASE WHEN t.ordflag = '0' THEN 1 END) AS 'ordfalg0',
                         COUNT(CASE WHEN t.ordflag = '1' THEN 1 END) AS 'ordfalg1',
                         COUNT(CASE WHEN t.ordflag = '2' THEN 1 END) AS 'ordfalg2',
                         COUNT(CASE WHEN t.ordflag = '3' THEN 1 END) AS 'ordfalg3',
                         COUNT(CASE WHEN t.ordflag = '4' THEN 1 END) AS 'ordfalg4',
                         COUNT(CASE WHEN t.ordflag = '5' THEN 1 END) AS 'ordfalg5',
                         COUNT(t.reqdate) AS 'totalCnt'
                     FROM DateSeries ds
                     LEFT JOIN TB_DA006W t
                         ON ds.reqdate = t.reqdate
                         AND t.spjangcd = :spjangcd
                     GROUP BY ds.reqdate
                     ORDER BY ds.reqdate;
           """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 작년 동일월 일별 데이터 조회
    public List<Map<String, Object>> LastYearCntOfDate(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH DateSeries AS (
            SELECT CONVERT(VARCHAR(8), DATEADD(YEAR, -1, DATEADD(DAY, number, CAST(DATEADD(DAY, 1 - DAY(GETDATE()), GETDATE()) AS DATE))), 112) AS last_year_reqdate
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND DAY(EOMONTH(DATEADD(YEAR, -1, GETDATE()))) - 1
        )
        SELECT
            ds.last_year_reqdate,
            COUNT(t.reqdate) AS last_year_totalCnt
        FROM DateSeries ds
        LEFT JOIN dbo.TB_DA006W t
            ON ds.last_year_reqdate = t.reqdate
            AND t.spjangcd = :spjangcd
        GROUP BY ds.last_year_reqdate
        ORDER BY ds.last_year_reqdate;
    """;

        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 올해 월별 상태값별 데이터 조회(전년대비 월별건수 포함)
    public List<Map<String, Object>> ThisYearCntOfOrdflagByMonth(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH MonthSeries AS (
            SELECT CONVERT(VARCHAR(6), DATEADD(MONTH, number, CAST(DATEADD(YEAR, DATEDIFF(YEAR, 0, GETDATE()), 0) AS DATE)), 112) AS month
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 11
        )
        SELECT
            ms.month AS reqmonth,
            COUNT(CASE WHEN t.ordflag = '0' THEN 1 END) AS ordfalg0,
            COUNT(CASE WHEN t.ordflag = '1' THEN 1 END) AS ordfalg1,
            COUNT(CASE WHEN t.ordflag = '2' THEN 1 END) AS ordfalg2,
            COUNT(CASE WHEN t.ordflag = '3' THEN 1 END) AS ordfalg3,
            COUNT(CASE WHEN t.ordflag = '4' THEN 1 END) AS ordfalg4,
            COUNT(CASE WHEN t.ordflag = '5' THEN 1 END) AS ordfalg5,
            COUNT(t.reqdate) AS totalCnt
        FROM MonthSeries ms
        LEFT JOIN TB_DA006W t
            ON ms.month = LEFT(t.reqdate, 6)
            AND t.spjangcd = :spjangcd
        GROUP BY ms.month
        ORDER BY ms.month;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 작년 월별 총 데이터 수 조회(전년대비 월별건수 포함)
    public List<Map<String, Object>> LastYearCntOfDateByMonth(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH MonthSeries AS (
            SELECT CONVERT(VARCHAR(6), DATEADD(MONTH, number, CAST(DATEADD(YEAR, DATEDIFF(YEAR, 0, GETDATE()) - 1, 0) AS DATE)), 112) AS last_year_month
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 11
        )
        SELECT
            ms.last_year_month AS last_year_reqmonth,
            COUNT(t.reqdate) AS last_year_totalCnt
        FROM MonthSeries ms
        LEFT JOIN TB_DA006W t
            ON ms.last_year_month = LEFT(t.reqdate, 6)
            AND t.spjangcd = :spjangcd
        GROUP BY ms.last_year_month
        ORDER BY ms.last_year_month;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 올해 분기별 상태값별 데이터 조회
    public List<Map<String, Object>> ThisYearCntOfOrdflagByQuarter(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH QuarterSeries AS (
            SELECT
                CONCAT(YEAR(GETDATE()), CEILING(MONTH(DATEADD(MONTH, number * 3, GETDATE())) / 3.0)) AS quarter
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 3  -- 4개(1~4분기)를 생성
        )
        SELECT 
            qs.quarter AS reqquarter,
            COUNT(CASE WHEN t.ordflag = '0' THEN 1 END) AS ordfalg0,
            COUNT(CASE WHEN t.ordflag = '1' THEN 1 END) AS ordfalg1,
            COUNT(CASE WHEN t.ordflag = '2' THEN 1 END) AS ordfalg2,
            COUNT(CASE WHEN t.ordflag = '3' THEN 1 END) AS ordfalg3,
            COUNT(CASE WHEN t.ordflag = '4' THEN 1 END) AS ordfalg4,
            COUNT(CASE WHEN t.ordflag = '5' THEN 1 END) AS ordfalg5,
            COUNT(t.reqdate) AS totalCnt
        FROM QuarterSeries qs
        LEFT JOIN TB_DA006W t
            ON CONCAT(YEAR(t.reqdate), CEILING(MONTH(t.reqdate) / 3.0)) = qs.quarter
            AND t.spjangcd = :spjangcd
        GROUP BY qs.quarter
        ORDER BY qs.quarter;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 작년 분기별 총 데이터 수 조회
    public List<Map<String, Object>> LastYearCntOfDateByQuarter(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
           WITH QuarterSeries AS (
                SELECT
                    CONCAT(YEAR(DATEADD(YEAR, -1, GETDATE())), 'Q', number + 1) AS last_year_quarter
                FROM master.dbo.spt_values
                WHERE type = 'P' AND number BETWEEN 0 AND 3  -- 1~4분기 강제 생성
            )
            SELECT
                qs.last_year_quarter AS last_year_reqquarter,
                COUNT(t.reqdate) AS last_year_totalCnt
            FROM QuarterSeries qs
            LEFT JOIN TB_DA006W t
                ON CONCAT(YEAR(t.reqdate), 'Q', CEILING(MONTH(t.reqdate) / 3.0)) = qs.last_year_quarter
                AND t.spjangcd = :spjangcd
            GROUP BY qs.last_year_quarter
            ORDER BY qs.last_year_quarter;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 최근 5년간 상태값별 데이터 조회
    public List<Map<String, Object>> LastFiveYearsCntOfOrdflag(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH YearSeries AS (
            SELECT CONVERT(VARCHAR(4), YEAR(GETDATE()) - number) AS year
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 4
        )
        SELECT 
            ys.year AS reqyear,
            COUNT(CASE WHEN t.ordflag = '0' THEN 1 END) AS ordfalg0,
            COUNT(CASE WHEN t.ordflag = '1' THEN 1 END) AS ordfalg1,
            COUNT(CASE WHEN t.ordflag = '2' THEN 1 END) AS ordfalg2,
            COUNT(CASE WHEN t.ordflag = '3' THEN 1 END) AS ordfalg3,
            COUNT(CASE WHEN t.ordflag = '4' THEN 1 END) AS ordfalg4,
            COUNT(CASE WHEN t.ordflag = '5' THEN 1 END) AS ordfalg5,
            COUNT(t.reqdate) AS totalCnt
        FROM YearSeries ys
        LEFT JOIN TB_DA006W t
            ON LEFT(t.reqdate, 4) = ys.year
            AND t.spjangcd = :spjangcd
        GROUP BY ys.year
        ORDER BY ys.year;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 YTD(올해)
    public List<Map<String, Object>> ThisYearCntOfStacked(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH MonthSeries AS (
            SELECT CONVERT(VARCHAR(6), DATEADD(MONTH, number, CAST(DATEADD(YEAR, DATEDIFF(YEAR, 0, GETDATE()), 0) AS DATE)), 112) AS month
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 11
        )
        SELECT
            ms.month AS reqmonth,
            (SELECT SUM(cnt)
             FROM (
                SELECT LEFT(reqdate, 6) AS reqmonth, COUNT(reqdate) AS cnt
                FROM TB_DA006W
                WHERE spjangcd = :spjangcd
                AND LEFT(reqdate, 4) = CONVERT(VARCHAR(4), GETDATE(), 112)  -- ✅ 올해(2025년) 데이터만 포함
                GROUP BY LEFT(reqdate, 6)
             ) t
             WHERE t.reqmonth <= ms.month
            ) AS totalCnt
        FROM MonthSeries ms
        ORDER BY ms.month;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }
    // 대시보드 YTD(작년)
    public List<Map<String, Object>> LastYearCntOfStacked(String spjangcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
        WITH MonthSeries AS (
            SELECT CONVERT(VARCHAR(6), DATEADD(MONTH, number, CAST(DATEADD(YEAR, DATEDIFF(YEAR, 0, GETDATE()) - 1, 0) AS DATE)), 112) AS last_year_month
            FROM master.dbo.spt_values
            WHERE type = 'P' AND number BETWEEN 0 AND 11
        )
        SELECT
            ms.last_year_month AS last_year_reqmonth,
            COALESCE((
            SELECT SUM(cnt)
             FROM (
                SELECT LEFT(reqdate, 6) AS last_year_reqmonth, COUNT(reqdate) AS cnt
                FROM TB_DA006W
                WHERE spjangcd = :spjangcd
                AND LEFT(reqdate, 4) = CONVERT(VARCHAR(4), DATEADD(YEAR, -1, GETDATE()), 112)  -- ✅ 작년(2024년) 데이터만 포함
                GROUP BY LEFT(reqdate, 6)
             ) t
             WHERE t.last_year_reqmonth <= ms.last_year_month
            ), 0) AS last_year_totalCnt
        FROM MonthSeries ms
        ORDER BY ms.last_year_month;
    """;
        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }






}
