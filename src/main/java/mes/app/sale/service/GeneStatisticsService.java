package mes.app.sale.service;

import mes.app.sale.GeneStatisticsController;
import mes.domain.repository.TB_RP320Repository;
import mes.domain.services.SqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeneStatisticsService {

	@Autowired
	private TB_RP320Repository tb_rp320Repository;

	@Autowired
	private SqlRunner sqlRunner;
	
	private static final Logger logger = LoggerFactory.getLogger(GeneStatisticsController.class);
	
	private List<Map<String, Object>> restructureData(List<Map<String, Object>> rawData, String powerid) {
		Map<String, Map<String, Object>> restructured = new HashMap<>();
		
		for (Map<String, Object> row : rawData) {
			String period = (String) row.get("period");
			String powernm = (String) row.get("powernm");
			String rowPowerId = (String) row.get("powerid"); // 추가된 부분
			BigDecimal totalValue = (BigDecimal) row.get("totalValue");
			
			// powerid가 'all'이거나, rowPowerId와 일치하는 경우
			if ("all".equals(powerid) || rowPowerId.equals(powerid)) {
				restructured.putIfAbsent(period, new HashMap<>());
				restructured.get(period).put("Period", period);
				restructured.get(period).put(powernm, totalValue);
			}
		}
		return new ArrayList<>(restructured.values());
	}
	
	public Map<String, Object> getPeriodicData(String startYearMonth, String endYearMonth, String powerid) {
		// 날짜 파싱 형식 지정
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
		YearMonth start = YearMonth.parse(startYearMonth, formatter);
		YearMonth end = YearMonth.parse(endYearMonth, formatter);
		
		// 시작 날짜와 종료 날짜 계산
		String startDate = start.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String endDate = end.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		
		System.out.println("Converted date range: " + startDate + " to " + endDate);
		
		// 각 기간별 데이터 계산
		Map<String, Object> result = new HashMap<>();
		result.put("monthly", restructureData(calculatePeriodicData(startDate, endDate, powerid, "YYYY-MM", "월별"), powerid));
		result.put("quarterly", restructureData(calculatePeriodicData(startDate, endDate, powerid, "YYYY-Q", "분기별"), powerid));
		result.put("halfYearly", restructureData(calculatePeriodicData(startDate, endDate, powerid, "YYYY-HH", "반기별"), powerid));
		result.put("yearly", restructureData(calculatePeriodicData(startDate, endDate, powerid, "YYYY", "연도별"), powerid));
		
		return result;
	}
	
	
	public Map<String, Object> getComparativeData(String startDate, String endDate, String powerid) {
		// 날짜 파싱 형식 지정
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
		YearMonth start = YearMonth.parse(startDate, formatter);
		YearMonth end = YearMonth.parse(endDate, formatter);
		
		// 시작 날짜와 종료 날짜 계산
		String formattedStartDate = start.atDay(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String formattedEndDate = end.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		
		Map<String, Object> result = new HashMap<>();
		
		// 대비 검색 결과에 restructureData 적용
		result.put("yoy", restructureData(getYoYData(formattedStartDate, formattedEndDate, powerid), powerid));
		result.put("qoq", restructureData(getQoQData(formattedStartDate, formattedEndDate, powerid), powerid));
		result.put("mom", restructureData(getMoMData(formattedStartDate, formattedEndDate, powerid), powerid));
		result.put("ytd", restructureData(getYTDData(formattedStartDate, formattedEndDate, powerid), powerid));
		
		return result;
	}
	
	
	//////
	private List<Map<String, Object>> calculatePeriodicData(String startDate, String endDate, String powerid, String dateFormat, String periodType) {
		StringBuilder sql = new StringBuilder("SELECT period, powerid, powernm, SUM(mevalue) as totalValue FROM (");
		sql.append("SELECT ");
		
		if ("분기별".equals(periodType)) {
			sql.append("FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY') || '-Q' || FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'Q') as period, ");
		} else if ("반기별".equals(periodType)) {
			sql.append("FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY') || '-H' || CASE WHEN EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) <= 6 THEN '1' ELSE '2' END as period, ");
		} else {
			sql.append("FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), '" + dateFormat + "') as period, ");
		}
		
		sql.append("t.powerid, t.powernm, t.mevalue ");
		sql.append("FROM tb_rp320 t ");
		sql.append("WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'yyyyMMdd') AND TO_DATE(:endDate, 'yyyyMMdd') ");
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		
		if (!"all".equals(powerid)) {
			sql.append("AND t.powerid = :powerid ");
			params.addValue("powerid", powerid);
		}
		
		sql.append(") AS subquery ");
		sql.append("GROUP BY period, powerid, powernm ");
		sql.append("ORDER BY period, powerid, powernm");
		
		logger.info("Executing SQL: {}", sql.toString());
		logger.info("With parameters: {}", params.getValues());
		
		return sqlRunner.getRows(sql.toString(), params);
	}



	// 월별 데이터 계산 로직
	private List<Map<String, Object>> getMonthlyData(String startDate, String endDate, String powerid) {
		StringBuilder sql = new StringBuilder(
				"SELECT FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
				"t.powernm, SUM(t.mevalue) as totalValue " +
				"FROM tb_rp320 t " +
				"WHERE t.standdt BETWEEN :startDate AND :endDate ");

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);

		if (powerid != null && !powerid.equals("all")) {
			sql.append("AND t.powerid = :powerid ");
			params.addValue("powerid", powerid);
		}

		sql.append("GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powernm " +
				   "ORDER BY month, t.powernm");

		return sqlRunner.getRows(sql.toString(), params);
	}

	// 분기별 데이터 계산 로직
	private List<Map<String, Object>> getQuarterlyData(String startDate, String endDate, String powerid) {
		String sql = "SELECT EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
					 "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0) as quarter, " +
					 "t.powernm, SUM(t.mevalue) as totalValue " +
					 "FROM tb_rp320 t " +
					 "WHERE t.standdt BETWEEN :startDate AND :endDate " +
					 (powerid != null ? "AND t.powerid = :powerid " : "") +
					 "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
					 "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0), t.powernm " +
					 "ORDER BY year, quarter, t.powernm";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}

		return sqlRunner.getRows(sql, params);
	}

	// 반기별 데이터 계산 로직
	private List<Map<String, Object>> getHalfYearlyData(String startDate, String endDate, String powerid) {
		String sql = "SELECT EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
					 "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 6.0) as half, " +
					 "t.powernm, SUM(t.mevalue) as totalValue " +
					 "FROM tb_rp320 t " +
					 "WHERE t.standdt BETWEEN :startDate AND :endDate " +
					 (powerid != null ? "AND t.powerid = :powerid " : "") +
					 "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
					 "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 6.0), t.powernm " +
					 "ORDER BY year, half, t.powernm";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}

		return sqlRunner.getRows(sql, params);
	}

	// 연도별 데이터 계산 로직
	private List<Map<String, Object>> getYearlyData(String startDate, String endDate, String powerid) {
		String sql = "SELECT EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
					 "t.powernm, SUM(t.mevalue) as totalValue " +
					 "FROM tb_rp320 t " +
					 "WHERE t.standdt BETWEEN :startDate AND :endDate " +
					 (powerid != null ? "AND t.powerid = :powerid " : "") +
					 "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), t.powernm " +
					 "ORDER BY year, t.powernm";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}

		return sqlRunner.getRows(sql, params);
	}

	// YoY 데이터 계산 로직
	private List<Map<String, Object>> getYoYData(String startDate, String endDate, String powerid) {
		String sql = "WITH yearly_data AS (" +
					 "  SELECT FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY') as year, " +
					 "         t.powerid, t.powernm, SUM(t.mevalue) as totalValue " +
					 "  FROM tb_rp320 t " +
					 "  WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'yyyyMMdd') AND TO_DATE(:endDate, 'yyyyMMdd') " +
					 (powerid != null && !powerid.equals("all") ? "  AND t.powerid = :powerid " : "") +
					 "  GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY'), t.powerid, t.powernm" +
					 ") " +
					 "SELECT d.year, d.powerid, d.powernm, " +
					 "  COALESCE((d.totalValue - LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.year)), 0) / " +
					 "  COALESCE(LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.year), 1) * 100 as yoy_change, " +
					 "  d.totalValue as totalValue " +  // totalValue도 함께 반환
					 "FROM yearly_data d " +
					 "ORDER BY d.year DESC, d.powerid";
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}
		
		return sqlRunner.getRows(sql, params);
	}
	
	
	// QoQ 데이터 계산 로직
	private List<Map<String, Object>> getQoQData(String startDate, String endDate, String powerid) {
		String sql = "WITH quarterly_data AS (" +
					 "  SELECT FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY') as year, " +
					 "         'Q' || FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'Q') as quarter, " +
					 "         t.powerid, t.powernm, SUM(t.mevalue) as totalValue " +
					 "  FROM tb_rp320 t " +
					 "  WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'yyyyMMdd') AND TO_DATE(:endDate, 'yyyyMMdd') " +
					 (powerid != null && !powerid.equals("all") ? "  AND t.powerid = :powerid " : "") +
					 "  GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY'), " +
					 "           'Q' || FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'Q'), t.powerid, t.powernm" +
					 ") " +
					 "SELECT d.year, d.quarter, d.powerid, d.powernm, " +
					 "  (d.totalValue - LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.year, d.quarter)) / LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.year, d.quarter) * 100 as qoq_change " +
					 "FROM quarterly_data d " +
					 "ORDER BY d.year DESC, d.quarter DESC, d.powerid";
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}
		
		return sqlRunner.getRows(sql, params);
	}
	
	// MoM 데이터 계산 로직
	private List<Map<String, Object>> getMoMData(String startDate, String endDate, String powerid) {
		String sql = "WITH monthly_data AS (" +
					 "  SELECT FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
					 "         t.powerid, t.powernm, SUM(t.mevalue) as totalValue " +
					 "  FROM tb_rp320 t " +
					 "  WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'yyyyMMdd') AND TO_DATE(:endDate, 'yyyyMMdd') " +
					 (powerid != null && !powerid.equals("all") ? "  AND t.powerid = :powerid " : "") +
					 "  GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powerid, t.powernm" +
					 ") " +
					 "SELECT d.month, d.powerid, d.powernm, " +
					 "  (d.totalValue - LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.month)) / LAG(d.totalValue) OVER (PARTITION BY d.powerid ORDER BY d.month) * 100 as mom_change " +
					 "FROM monthly_data d " +
					 "ORDER BY d.month DESC, d.powerid";
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}
		
		return sqlRunner.getRows(sql, params);
	}
	
	// YTD 데이터 계산 로직
	private List<Map<String, Object>> getYTDData(String startDate, String endDate, String powerid) {
		String sql = "WITH ytd_data AS (" +
					 "  SELECT FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
					 "         t.powerid, t.powernm, SUM(t.mevalue) as totalValue " +
					 "  FROM tb_rp320 t " +
					 "  WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'yyyyMMdd') AND TO_DATE(:endDate, 'yyyyMMdd') " +
					 (powerid != null && !powerid.equals("all") ? "  AND t.powerid = :powerid " : "") +
					 "  GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powerid, t.powernm" +
					 ") " +
					 "SELECT d.month, d.powerid, d.powernm, " +
					 "  SUM(d.totalValue) OVER (PARTITION BY d.powerid, EXTRACT(YEAR FROM TO_DATE(d.month, 'YYYY-MM')) ORDER BY d.month) as cumulativeValue " +
					 "FROM ytd_data d " +
					 "ORDER BY d.month DESC, d.powerid";
		
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("startDate", startDate);
		params.addValue("endDate", endDate);
		if (powerid != null) {
			params.addValue("powerid", powerid);
		}
		
		return sqlRunner.getRows(sql, params);
	}
	
}
