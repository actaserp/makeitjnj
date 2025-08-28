package mes.app.order_status.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VendorOrderStatsService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getOrderStatusByOperid(String startDate, String endDate, String searchSpjangcd, String searchCltnm) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder("""
        SELECT
            tb006.*,
            uc.Value AS ordflag_display
        FROM
            TB_DA006W tb006
        left join user_code uc on uc.Code = tb006.ordflag
        WHERE 1=1
        """);
    if (startDate != null && !startDate.isEmpty()) {
      startDate = startDate.replace("-", "");
      sql.append(" AND reqdate >= :startDate");
      params.addValue("startDate", startDate);
    }

    if (endDate != null && !endDate.isEmpty()) {
      sql.append(" AND reqdate <= :endDate");
      params.addValue("endDate", endDate);
    }
    if (searchSpjangcd != null && !searchSpjangcd.isEmpty()) {
      sql.append(" AND spjangcd = :spjangcd");
      params.addValue("spjangcd", searchSpjangcd);
    }

    if (searchCltnm != null && !searchCltnm.isEmpty()) {
      sql.append(" AND cltnm LIKE :cltnm");
      params.addValue("cltnm", "%" + searchCltnm + "%");
    }
    sql.append(" ORDER BY reqdate DESC");

    //log.info("업체별 주문통계 그리드 read SQL: {}", sql.toString());
    //log.info("바인딩된 파라미터: {}", params.getValues());

    return sqlRunner.getRows(sql.toString(), params);
  }

  public List<Map<String, Object>> getChartData(String spjangcd, String startDate, String endDate, String searchCltnm) {

    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder("""
        SELECT cltnm, ordflag
        FROM tb_da006w
        WHERE 1=1
        """);

    if (spjangcd != null && !spjangcd.isEmpty()) {
      sql.append(" AND spjangcd = :spjangcd");
      params.addValue("spjangcd", spjangcd);
    }

    if (startDate != null && !startDate.isEmpty()) {
      startDate = startDate.replace("-", "");
      sql.append(" AND reqdate >= :startDate");
      params.addValue("startDate", startDate);
    }

    if (endDate != null && !endDate.isEmpty()) {
      sql.append(" AND reqdate <= :endDate");
      params.addValue("endDate", endDate);
    }

    if (searchCltnm != null && !searchCltnm.isEmpty()) {
      sql.append(" AND cltnm LIKE :cltnm");
      params.addValue("cltnm", "%" + searchCltnm + "%");
    }
    sql.append(" ORDER BY reqdate DESC");

    //log.info("업체별 주문통계 차트 read SQL: {}", sql.toString());
    //log.info("바인딩된 파라미터: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

  public List<Map<String, Object>> getSalesList(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("cboCompany", cboCompany);
    paramMap.addValue("spjangcd", spjangcd);

    String data_year = cboYear;
    paramMap.addValue("date_form", data_year + "0101");
    paramMap.addValue("date_to", data_year + "1231");

    StringBuilder sql = new StringBuilder();
    // 1) 대상 행 선별 (연/월 파싱)
    sql.append("""
    WITH base AS (
      SELECT
        t.cltcd,
        t.spjangcd,
        ISNULL(t.amount, 0) AS amount,
        SUBSTRING(t.reqdate, 1, 4) AS sales_year,
        SUBSTRING(t.reqdate, 5, 2) AS sales_month
      FROM ELV_JNJ.dbo.TB_DA006W t
      WHERE t.spjangcd = :spjangcd
        AND SUBSTRING(t.reqdate, 1, 4) = :cboYear
  """);

    // 회사(거래처) 필터: company.id가 정수인 경우
    if (cboCompany != null) {
      sql.append("    AND TRY_CAST(t.cltcd AS int) = :cboCompany\n");
    }
    sql.append(")\n");

    // 2) 거래처별·월별 집계
    sql.append("""
    SELECT
      b.cltcd,
      COALESCE(c.[Name], b.cltcd) AS comp_name,
  """);

    // 월별 합계 mon_1 ~ mon_12
    for (int i = 1; i <= 12; i++) {
      String mm = String.format("%02d", i);
      sql.append("  SUM(CASE WHEN b.sales_month = '").append(mm)
          .append("' THEN b.amount ELSE 0 END) AS mon_").append(i);
      sql.append(i < 12 ? ",\n" : "\n");
    }

    // 총합계
    sql.append("""
      , SUM(b.amount) AS total_sum
    FROM base b
    LEFT JOIN company c
      ON c.id = TRY_CAST(b.cltcd AS int)
    GROUP BY b.cltcd, c.[Name]
    ORDER BY comp_name, b.cltcd
  """);

//    log.info("월별 매출현황 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), paramMap);
    return items;
  }

  public List<Map<String, Object>> getPurchaseList(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);      // 예: "2025"
    paramMap.addValue("spjangcd", spjangcd);    // 예: "ZZ"
    paramMap.addValue("date_form", cboYear + "0101");
    paramMap.addValue("date_to",   cboYear + "1231");
    if (cboCompany != null) paramMap.addValue("cboCompany", cboCompany);

    StringBuilder sql = new StringBuilder();

    // 1) 대상 행 선별 (연/월 파싱) + 헤더와 조인하여 업체(거래처) 코드 확보
    sql.append("""
    WITH base AS (
      SELECT
        j.clttype,                      -- 외주처 구분(가정: 외주처 표기용)
        ISNULL(j.setamt, 0) AS purchase_amt,  -- 매입액(집계용)
        ISNULL(j.uamt, 0)   AS unit_cost,     -- 단가(원가)
        j.reqdate,
        SUBSTRING(j.reqdate, 1, 4) AS y,
        SUBSTRING(j.reqdate, 5, 2) AS m
      FROM ELV_JNJ.dbo.TB_DA007W j
      INNER JOIN ELV_JNJ.dbo.TB_DA006W h
        ON h.custcd  = j.custcd
       AND h.spjangcd= j.spjangcd
       AND h.reqdate = j.reqdate
       AND h.reqnum  = j.reqnum
      WHERE j.spjangcd = :spjangcd
        AND j.reqdate BETWEEN :date_form AND :date_to
        AND j.clttype IS NOT NULL AND LTRIM(RTRIM(j.clttype)) <> ''
  """);

    // 거래처(업체) 필터: company.id가 정수인 경우 TRY_CAST 사용
    if (cboCompany != null) {
      sql.append("        AND TRY_CAST(j.clttype AS int) = :cboCompany\n");
    }
    sql.append(")\n");

    // 2) 업체·주문일자(대표)·외주처·자재별 월 피벗 집계
    sql.append("""
    SELECT
      MIN(b.reqdate) , 
      b.clttype  , 
      c2.Name as clt_name ,
      MAX(b.unit_cost) AS [매입액(1set/원가)],
  """);

    // 월별 합계 mon_1 ~ mon_12
    for (int i = 1; i <= 12; i++) {
      String mm = String.format("%02d", i);
      sql.append("  SUM(CASE WHEN b.m = '").append(mm)
          .append("' THEN b.purchase_amt ELSE 0 END) AS mon_").append(i);
      sql.append(i < 12 ? ",\n" : "\n");
    }

    // 총합(원하시면 포함)
    sql.append("""
    , SUM(b.purchase_amt) AS total_sum
     FROM base b
     left join company c2 on b.clttype = c2.id
     GROUP BY b.clttype, c2.Name
     ORDER BY clt_name
  """);

//     log.info("월별 매입현황 SQL: {}", sql);
//     log.info("SQL Parameters: {}", paramMap.getValues());
    return this.sqlRunner.getRows(sql.toString(), paramMap);
  }

  public List<Map<String, Object>> getSalesChartRead(String cboYear, Integer cboCompany, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);
    paramMap.addValue("spjangcd", spjangcd);

    int prevYear = Integer.parseInt(cboYear) - 1;
    paramMap.addValue("prevYear", String.valueOf(prevYear));
    if (cboCompany != null) paramMap.addValue("cboCompany", cboCompany);

    StringBuilder sql = new StringBuilder();
    sql.append("""
      WITH months AS (
        SELECT '01' AS m UNION ALL SELECT '02' UNION ALL SELECT '03' UNION ALL SELECT '04'
        UNION ALL SELECT '05' UNION ALL SELECT '06' UNION ALL SELECT '07' UNION ALL SELECT '08'
        UNION ALL SELECT '09' UNION ALL SELECT '10' UNION ALL SELECT '11' UNION ALL SELECT '12'
      ),
      base AS (
        SELECT
          t.spjangcd,
          ISNULL(t.amount, 0) AS amount,
          SUBSTRING(t.reqdate, 1, 4) AS sales_year,
          SUBSTRING(t.reqdate, 5, 2) AS sales_month
        FROM ELV_JNJ.dbo.TB_DA006W t
        WHERE t.spjangcd = :spjangcd
          AND SUBSTRING(t.reqdate, 1, 4) IN (:cboYear, :prevYear)
    """);

    if (cboCompany != null) {
      sql.append("  AND TRY_CAST(t.cltcd AS int) = :cboCompany\n");
    }
    sql.append(")\n");

    sql.append("""
      , agg AS (
        SELECT
          sales_month,
          SUM(CASE WHEN sales_year = :cboYear  THEN amount ELSE 0 END) AS cur_year_amount,
          SUM(CASE WHEN sales_year = :prevYear THEN amount ELSE 0 END) AS prev_year_amount
        FROM base
        GROUP BY sales_month
      )
      SELECT
        m.m AS month,
        ISNULL(a.cur_year_amount, 0)  AS cur_year_amount,
        ISNULL(a.prev_year_amount, 0) AS prev_year_amount
      FROM months m
      LEFT JOIN agg a ON a.sales_month = m.m
      ORDER BY m.m
    """);

    return this.sqlRunner.getRows(sql.toString(), paramMap);
  }

  public List<Map<String, Object>> getDepositChartRead(String cboYear, Integer cboCompany, String spjangcd) {

    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cboYear", cboYear);      // 예: "2025"
    paramMap.addValue("spjangcd", spjangcd);    // 예: "ZZ"

    int prevYear = Integer.parseInt(cboYear) - 1;
    paramMap.addValue("prevYear", String.valueOf(prevYear));
    if (cboCompany != null) paramMap.addValue("cboCompany", cboCompany);

    StringBuilder sql = new StringBuilder();
    sql.append("""
      WITH months AS (
        SELECT '01' AS m UNION ALL SELECT '02' UNION ALL SELECT '03' UNION ALL SELECT '04'
        UNION ALL SELECT '05' UNION ALL SELECT '06' UNION ALL SELECT '07' UNION ALL SELECT '08'
        UNION ALL SELECT '09' UNION ALL SELECT '10' UNION ALL SELECT '11' UNION ALL SELECT '12'
      ),
      base AS (
        SELECT
          j.clttype,                     
          j.spjangcd,
          ISNULL(j.setamt, 0) AS purchase_amt,  -- 매입액(집계용)
          SUBSTRING(j.reqdate, 1, 4) AS y,
          SUBSTRING(j.reqdate, 5, 2) AS m
        FROM ELV_JNJ.dbo.TB_DA007W j
        INNER JOIN ELV_JNJ.dbo.TB_DA006W h
          ON h.custcd  = j.custcd
         AND h.spjangcd= j.spjangcd
         AND h.reqdate = j.reqdate
         AND h.reqnum  = j.reqnum
        WHERE j.spjangcd = :spjangcd
          AND SUBSTRING(j.reqdate, 1, 4) IN (:cboYear, :prevYear)
           AND j.clttype IS NOT NULL AND LTRIM(RTRIM(j.clttype)) <> ''
    """);

    if (cboCompany != null) {
      sql.append("      AND TRY_CAST(j.clttype AS int) = :cboCompany\n");
    }
    sql.append("""
      ),
      agg AS (
        SELECT
          m,
          SUM(CASE WHEN y = :cboYear  THEN purchase_amt ELSE 0 END) AS cur_year_amount,
          SUM(CASE WHEN y = :prevYear THEN purchase_amt ELSE 0 END) AS prev_year_amount
        FROM base
        GROUP BY m
      )
      SELECT
        mo.m AS month,
        ISNULL(a.cur_year_amount, 0)  AS cur_year_amount,
        ISNULL(a.prev_year_amount, 0) AS prev_year_amount
      FROM months mo
      LEFT JOIN agg a ON a.m = mo.m
      ORDER BY mo.m
    """);

//     log.info("전년대비 월별 매입차트 SQL: {}", sql);
//     log.info("SQL Parameters: {}", paramMap.getValues());
    return this.sqlRunner.getRows(sql.toString(), paramMap);

  }
}
