package mes.app.ProgressStatus.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProgressStatusService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getProgressStatusList(
        String search_spjangcd ,String startDate, String endDate,
        String searchCltnm,String searchtketnm,String searchTitle) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("spjangcd", search_spjangcd);
        params.addValue("startDate", startDate);
        params.addValue("endDate", endDate);
        StringBuilder sql = new StringBuilder("""
        SELECT
            tb006.*,
            uc.Value AS ordflag_display
        FROM
            TB_DA006W tb006
        left join sys_code uc on uc.Code = tb006.ordflag
        WHERE 1=1
        """);
        if (startDate != null && !startDate.isEmpty()) {
            startDate = startDate.replace("-", ""); 
            sql.append(" AND reqdate >= :startDate ");
            params.addValue("startDate", startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND reqdate <= :endDate ");
            params.addValue("endDate", endDate);
        }
        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            sql.append(" AND cltnm LIKE :cltnm");
            params.addValue("cltnm", "%" + searchCltnm + "%");
        }
        if (search_spjangcd != null && !search_spjangcd.isEmpty()) {
            sql.append(" AND spjangcd = :spjangcd");
            params.addValue("spjangcd", search_spjangcd);
        }

        if (searchtketnm != null && !searchtketnm.equals("all") && !searchtketnm.isEmpty()) {
            sql.append(" AND ordflag = :ordflag");
            params.addValue("ordflag", searchtketnm);
        }

        if (searchTitle != null && !searchTitle.isEmpty()) {
            sql.append(" AND modeltxt LIKE :searchTitle");
            params.addValue("searchTitle", "%" + searchTitle + "%");
        }
        // ORDER BY를 항상 맨 마지막에 추가
        sql.append(" ORDER BY reqdate DESC");

        //log.info("진행현황 그리드 SQL: {}", sql);
        //log.info("SQL Parameters: {}", params.getValues());
        return sqlRunner.getRows(sql.toString(), params);
    }


    public List<Map<String, Object>> getChartData2(String search_spjangcd, String startDate, String endDate,
            String searchCltnm, String searchtketnm, String searchTitle) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
        SELECT cltnm, ordflag
        FROM tb_da006w
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

        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            sql.append(" AND cltnm LIKE :cltnm");
            params.addValue("cltnm", "%" + searchCltnm + "%");
        }
        if (search_spjangcd != null && !search_spjangcd.isEmpty()) {
            sql.append(" AND spjangcd = :spjangcd");
            params.addValue("spjangcd", search_spjangcd);
        }

        if (searchtketnm != null && !searchtketnm.equals("all") && !searchtketnm.isEmpty()) {
            sql.append(" AND ordflag = :ordflag");
            params.addValue("ordflag", searchtketnm);
        }

        if (searchTitle != null && !searchTitle.isEmpty()) {
            sql.append(" AND remark LIKE :searchTitle");
            params.addValue("searchTitle", "%" + searchTitle + "%");
        }

        //log.info("실행될 SQL: {}", sql.toString());
        //log.info("바인딩된 파라미터: {}", params.getValues());

        return sqlRunner.getRows(sql.toString(), params);
    }

}
