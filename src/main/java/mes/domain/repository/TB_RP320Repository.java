package mes.domain.repository;

import mes.domain.entity.actasEntity.TB_RP320;
import mes.domain.entity.actasEntity.TB_RP320_Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TB_RP320Repository extends JpaRepository<TB_RP320, TB_RP320_Id> {
	
	Optional<TB_RP320> findBySpworkcdAndSpcompcdAndSpplancdAndStanddtAndPoweridAndPowtime(
			String spworkcd, String spcompcd, String spplancd, String standdt, String powerid, Integer powtime);
	
	// 발전기 코드 목록 가져오기
	@Query("SELECT DISTINCT t.powerid FROM TB_RP320 t")
	List<String> findDistinctPowerIds();
	
	// 등록 일지 목록 가져오기
	@Query(value = "SELECT LEFT(standdt, 7) AS yymm \n"
				   + "FROM TB_RP320 \n"
				   + "GROUP BY LEFT(standdt, 7) \n"
				   + "ORDER BY LEFT(standdt, 7) DESC;\n", nativeQuery = true)
	List<String> findDistinctYearMonths();
	

	// tab3
	@Query("SELECT MAX(t.standdt) FROM TB_RP320 t")
	String findLatestDate();
	
	List<TB_RP320> findByStanddt(String standdt);
	
	// tab4
	
	// 월별
	@Query(value = "SELECT " +
				   "FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'YYYYMMDD') AND TO_DATE(:endDate, 'YYYYMMDD') " +
				   "GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powernm " +
				   "ORDER BY month, t.powernm", nativeQuery = true)
	List<Object[]> getMonthlyData(@Param("startDate") String startDate,
								  @Param("endDate") String endDate);
	
	// 분기별
	@Query(value = "SELECT " +
				   "EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0) as quarter, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'YYYYMMDD') AND TO_DATE(:endDate, 'YYYYMMDD') " +
				   "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0), " +
				   "t.powernm " +
				   "ORDER BY year, quarter, t.powernm", nativeQuery = true)
	List<Object[]> getQuarterlyData(@Param("startDate") String startDate,
									@Param("endDate") String endDate);

	// 반기별
	@Query(value = "SELECT " +
				   "EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 6.0) as half, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'YYYYMMDD') AND TO_DATE(:endDate, 'YYYYMMDD') " +
				   "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 6.0), " +
				   "t.powernm " +
				   "ORDER BY year, half, t.powernm", nativeQuery = true)
	List<Object[]> getHalfYearlyData(@Param("startDate") String startDate,
									 @Param("endDate") String endDate);
	
	// 연도별
	@Query(value = "SELECT " +
				   "EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startDate, 'YYYYMMDD') AND TO_DATE(:endDate, 'YYYYMMDD') " +
				   "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
				   "t.powernm " +
				   "ORDER BY year, t.powernm", nativeQuery = true)
	List<Object[]> getYearlyData(@Param("startDate") String startDate,
								 @Param("endDate") String endDate);
	
	
	
	@Query(value = "SELECT MIN(standdt) FROM tb_rp320", nativeQuery = true)
	String findMinDate();
	
	@Query(value = "SELECT MAX(standdt) FROM tb_rp320", nativeQuery = true)
	String findMaxDate();
	
	// tab5
	// YoY MoM
	@Query(value = "SELECT " +
				   "FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powernm " +
				   "ORDER BY month, t.powernm", nativeQuery = true)
	List<Object[]> getMonthlyDataForYoYAndMoM();
	
	// QoQ
	@Query(value = "SELECT " +
				   "EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) as year, " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0) as quarter, " +
				   "t.powernm, " +
				   "SUM(t.mevalue) as totalValue " +
				   "FROM tb_rp320 t " +
				   "GROUP BY EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')), " +
				   "CEIL(EXTRACT(MONTH FROM TO_DATE(t.standdt, 'YYYYMMDD')) / 3.0), " +
				   "t.powernm " +
				   "ORDER BY year, quarter, t.powernm", nativeQuery = true)
	List<Object[]> getQuarterlyDataForQoQ();
	
	// YTD
	@Query(value = "SELECT " +
				   "FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM') as month, " +
				   "t.powernm, " +
				   "SUM(SUM(t.mevalue)) OVER (PARTITION BY t.powernm, EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) ORDER BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM')) as cumulativeValue " +
				   "FROM tb_rp320 t " +
				   "GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYYMMDD'), 'YYYY-MM'), t.powernm, EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYYMMDD')) " +
				   "ORDER BY month, t.powernm", nativeQuery = true)
	List<Object[]> getMonthlyDataForYTD();
	
	// 검색 (tab1, tab2)
	@Query(value = "SELECT t.* FROM tb_rp320 t WHERE TO_DATE(t.standdt, 'YYYYMMDD') BETWEEN TO_DATE(:startdt, 'YYYY-MM-DD') AND TO_DATE(:enddt, 'YYYY-MM-DD')"
				   + " AND (:powerid IS NULL OR t.powerid = :powerid)", nativeQuery = true)
	List<TB_RP320> searchGeneData(@Param("startdt") String startdt,
								  @Param("enddt") String enddt,
								  @Param("powerid") String powerid);
	
	// 검색 (tab3)
	@Query(value = "SELECT t.* FROM tb_rp320 t WHERE TO_DATE(t.standdt, 'YYYYMMDD') = TO_DATE(:date, 'YYYY-MM-DD') "
				   + "AND (:powerid IS NULL OR t.powerid = :powerid)", nativeQuery = true)
	List<TB_RP320> searchGeneDataByDate(@Param("date") String date,
										@Param("powerid") String powerid);
	
	
	// spworkcd, spcompcd, spplancd, standdt, powerid로 조회
//	Optional<TB_RP320> findBySpworkcdAndSpcompcdAndSpplancdAndStanddtAndPowerid(
//			String spworkcd, String spcompcd, String spplancd, String standdt, String powerid);


//	// INDATEM으로 데이터 검색 (시작 시간과 종료 시간 사이의 데이터 조회)
//	@Query("SELECT t FROM TB_RP320 t WHERE t.INDATEM BETWEEN :start AND :end")
//	List<TB_RP320> findByIndatemBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
//

//
//	// 발전기명 목록 가져오기
//	@Query("SELECT DISTINCT t.powernm FROM TB_RP320 t")
//	List<String> findDistinctGeneratorNames();
//
//	// INDATEM으로 데이터 검색
////	List<TB_RP320> findByIndatem(LocalDate INDATEM);
//
//
//	@Query(value = "SELECT * FROM TB_RP320 t WHERE t.standdt BETWEEN :startdt AND :enddt " +
//			"AND (:powerid IS NULL OR t.powerid = :powerid)", nativeQuery = true)
//	List<TB_RP320> searchGeneData(@Param("startdt") String startdt, @Param("enddt") String enddt, @Param("powerid") String powerid);


//
//	List<TB_RP320> findByIndatem(LocalDate INDATEM);
//
//
//
//	// 기간별 발전량 조회
//
//	// 시간별 조회
//	@Query(value = "SELECT t.standdt AS period, t.powernm, " +
//			"t.mevalue01 AS hour01, t.mevalue02 AS hour02, t.mevalue03 AS hour03, t.mevalue04 AS hour04, " +
//			"t.mevalue05 AS hour05, t.mevalue06 AS hour06, t.mevalue07 AS hour07, t.mevalue08 AS hour08, " +
//			"t.mevalue09 AS hour09, t.mevalue10 AS hour10, t.mevalue11 AS hour11, t.mevalue12 AS hour12, " +
//			"t.mevalue13 AS hour13, t.mevalue14 AS hour14, t.mevalue15 AS hour15, t.mevalue16 AS hour16, " +
//			"t.mevalue17 AS hour17, t.mevalue18 AS hour18, t.mevalue19 AS hour19, t.mevalue20 AS hour20, " +
//			"t.mevalue21 AS hour21, t.mevalue22 AS hour22, t.mevalue23 AS hour23, t.mevalue24 AS hour24 " +
//			"FROM TB_RP320 t " +
//			"WHERE t.standdt = :date " +
//			"AND (:powerid IS NULL OR :powerid = 'all' OR t.powernm = :powerid)", nativeQuery = true)
//	List<Map<String, Object>> searchHourlyData(@Param("date") String date, @Param("powerid") String powerid);
//
//
//	// 월별 조회, 분기별 조회, 반기별 조회
////	@Query("SELECT new mes.domain.DTO.TB_RP320Dto(" +
////			"FORMAT(DATE_TRUNC('month', TO_DATE(t.standdt, 'YYYY-MM-DD')), 'YYYY-MM'), " +
////			"t.powerid, t.powernm, SUM(t.mevaluet)) " +
////			"FROM TB_RP320 t " +
////			"WHERE TO_DATE(t.standdt, 'YYYY-MM-DD') BETWEEN TO_DATE(:startdt, 'YYYY-MM-DD') AND TO_DATE(:enddt, 'YYYY-MM-DD') " +
////			"AND (:powerid = 'all' OR t.powernm = :powerid) " +
////			"GROUP BY DATE_TRUNC('month', TO_DATE(t.standdt, 'YYYY-MM-DD')), t.powerid, t.powernm")
////	List<TB_RP320Dto> searchMonthlyData(@Param("startdt") String startdt, @Param("enddt") String enddt, @Param("powerid") String powerid);
//
//
//
//	// 분기별 조회
//	@Query("SELECT new mes.domain.DTO.TB_RP320Dto(" +
//			"FORMAT(DATE_TRUNC('month', TO_DATE(t.standdt, 'YYYY-MM-DD')), 'YYYY-MM'), " +
//			"t.powerid, t.powernm, SUM(t.mevaluet)) " +
//			"FROM TB_RP320 t " +
//			"WHERE TO_DATE(t.standdt, 'YYYY-MM-DD') BETWEEN TO_DATE(:startdt, 'YYYY-MM-DD') AND TO_DATE(:enddt, 'YYYY-MM-DD') " +
//			"AND (:powerid = 'all' OR t.powernm = :powerid) " +
//			"GROUP BY DATE_TRUNC('month', TO_DATE(t.standdt, 'YYYY-MM-DD')), t.powerid, t.powernm")
//	List<TB_RP320Dto> searchQuarterlyData(@Param("startdt") String startdt, @Param("enddt") String enddt, @Param("powerid") String powerid);
//
//
//	// 연도별 조회
//	@Query("SELECT new mes.domain.DTO.TB_RP320Dto(" +
//		   "FORMAT(TO_DATE(t.standdt, 'YYYY-MM-DD'), 'YYYY-MM'), " +
//		   "t.powerid, t.powernm, SUM(t.mevalue)) " +
//		   "FROM TB_RP320 t " +
//		   "WHERE EXTRACT(YEAR FROM TO_DATE(t.standdt, 'YYYY-MM-DD')) = :year " +
//		   "AND (:powerid = 'all' OR t.powernm = :powerid) " +
//		   "GROUP BY FORMAT(TO_DATE(t.standdt, 'YYYY-MM-DD'), 'YYYY-MM'), t.powerid, t.powernm " +
//		   "ORDER BY FORMAT(TO_DATE(t.standdt, 'YYYY-MM-DD'), 'YYYY-MM')")
//	List<TB_RP320Dto> searchYearlyData(@Param("year") int year, @Param("powerid") String powerid);
//
//
//
//	// 연도 목록 가져오기
//	@Query("SELECT DISTINCT FORMAT(TO_DATE(t.standdt, 'YYYY-MM-DD'), 'YYYY') AS year " +
//			"FROM TB_RP320 t " +
//			"ORDER BY FORMAT(TO_DATE(t.standdt, 'YYYY-MM-DD'), 'YYYY') DESC")
//	List<String> findDistinctYears();
//
//
//
//	@Query("SELECT MIN(TO_DATE(standdt, 'YYYY-MM-DD')) FROM TB_RP320")
//	String findMinDate();
//
//	@Query("SELECT MAX(TO_DATE(standdt, 'YYYY-MM-DD')) FROM TB_RP320")
//	String findMaxDate();



}
