package mes.app.order_dashboard.service;

import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderDashboardService {

    @Autowired
    SqlRunner sqlRunner;

    // username으로 cltcd, cltnm, saupnum, custcd 가지고 오기
    public Map<String, Object> getUserInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
            select xc.custcd,
              xc.cltcd,
              xc.cltnm,
              c.id as comp_id,
              xc.saupnum,
              au.spjangcd
            FROM TB_XCLIENT xc
            left join auth_user au on au."username" = xc.saupnum
            left join company c on xc.cltcd =c.Code\s
            WHERE xc.saupnum = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    //주문의뢰현황 불러오기
    public List<Map<String, Object>> getOrderList(TB_DA006W_PK tbDa006W_pk,
                                                  String searchStartDate, String searchEndDate, String searchType, String saupnum, String cltcd) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        dicParam.addValue("searchType", searchType);
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());
        dicParam.addValue("saupnum", saupnum);
        dicParam.addValue("cltcd", cltcd);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    custcd,
                    spjangcd,
                    reqnum,
                    reqdate,
                    ordflag,
                    deldate,
                    telno,
                    perid,
                    cltzipcd,
                    cltaddr,
                    modeltxt
                FROM
                    TB_DA006W hd
                WHERE
                    hd.custcd = :custcd
                    AND hd.spjangcd = :spjangcd
                    AND hd.saupnum = :saupnum
                    AND hd.cltcd = :cltcd
                """);
        // 날짜 필터
        if (searchStartDate != null && !searchStartDate.isEmpty()) {
            sql.append(" AND reqdate >= :searchStartDate");
        }
        //
        if (searchEndDate != null && !searchEndDate.isEmpty()) {
            sql.append(" AND reqdate <= :searchEndDate");
        }
        // 진행구분 필터
        if (searchType != null && !searchType.isEmpty()) {
            sql.append(" AND ordflag LIKE :searchType");
        }
        // 정렬 조건 추가
        sql.append(" ORDER BY reqdate ASC");

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }

    public List<Map<String, Object>> initDatas(TB_DA006W_PK tbDa006WPk, String cltcd) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.ordflag,
                    COUNT(*) AS ordflag_count
                FROM
                    TB_DA006W hd
                WHERE
                    hd.spjangcd = :spjangcd
                    AND hd.cltcd = :cltcd
                    AND LEFT(hd.reqdate, 4) = CAST(YEAR(GETDATE()) AS VARCHAR(4))
                GROUP BY
                    hd.ordflag;
                """);
        dicParam.addValue("custcd", tbDa006WPk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006WPk.getSpjangcd());
        dicParam.addValue("cltcd", cltcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    // 주문현황 캘린더
    public List<Map<String, Object>> getOrderList2(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    custcd,
                    spjangcd,
                    reqnum,
                    reqdate,
                    ordflag,
                    deldate,
                    telno,
                    perid,
                    cltzipcd,
                    cltaddr,
                    remark
                FROM
                    TB_DA006W hd
                WHERE
                    hd.saupnum = :username
                    AND hd.reqdate BETWEEN
                        CAST(CAST(YEAR(GETDATE()) - 1 AS VARCHAR(4)) + '0101' AS INT)
                        AND CAST(CAST(YEAR(GETDATE()) AS VARCHAR(4)) + '1231' AS INT)
                """);
        // 정렬 조건 추가
        sql.append(" ORDER BY reqdate ASC");

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
}
