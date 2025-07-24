package mes.app.request.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_DA006WFile;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RequestService {

  @Autowired
  SqlRunner sqlRunner;

  @Autowired
  TB_DA006WFILERepository tbDa006WFILERepository;

  //BOM 리스트
  public List<Map<String, Object>> getBomList(Integer materialId) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("Material_id", materialId);
    String sql = """
        select b.id from bom b 
        left join material m on m.id = b.Material_id
        where m.id = :Material_id
        """;
    return sqlRunner.getRows(sql, param);
  }

  public List<Map<String, Object>> getHoliday(){
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    String sql = """
               select * from TB_PZ010
                """;

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
    return items;
  }

  public boolean saveFile(TB_DA006WFile tbDa006WFile) {
    try {
      tbDa006WFILERepository.save(tbDa006WFile);
      return true;

    } catch (Exception e) {
      System.out.println(e + ": 에러발생");
      return false;
    }
  }

  public List<Map<String, Object>> getTab2Read(Integer compcd , String ordflag, Timestamp start, Timestamp end, String spjangcd) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("compcd", compcd);
    param.addValue("ordflag", ordflag);
    param.addValue("start", start);
    param.addValue("end", end);
    param.addValue("spjangcd", spjangcd);

    String sql = """
        WITH aggregated AS (
          SELECT *
          FROM (
            SELECT
              reqdate,
              reqnum,
              reqseq,
              pname,
              remark,
              qty,
              saleamt,
              uamt,
              ROW_NUMBER() OVER (PARTITION BY reqdate, reqnum ORDER BY reqseq ASC) AS rn
            FROM TB_DA007W
          ) AS sub
          WHERE rn = 1  -- 가장 작은 reqseq만 남김
        ),
        summary AS (
          SELECT
            reqdate,
            reqnum,
            SUM(qty) AS total_qty,
            SUM(saleamt) AS total_saleamt,
            SUM(uamt) AS total_uamt
          FROM TB_DA007W
          GROUP BY reqdate, reqnum
        )
        SELECT
          h.reqnum,
          STUFF(STUFF(h.indate, 5, 0, '-'), 8, 0, '-') AS indate,
          CASE h.ordflag
            WHEN 0 THEN '주문의뢰'
            WHEN 1 THEN '견적작성'
            WHEN 2 THEN '제작'
            WHEN 3 THEN '출고'
          END AS ordflag,
          h.cltcd,
          h.cltnm,
          h.indate,
          h.modeltxt,
          m.Name AS model_naem,
          s.total_qty,
          s.total_saleamt,
          s.total_uamt,
          a.remark,
          a.pname,
          a.reqseq,
          STUFF(STUFF(h.deldate, 5, 0, '-'), 8, 0, '-') AS deldate,
          (
             SELECT bd.filepath, bd.filesvnm, bd.fileextns,
             bd.fileurl, bd.fileornm, bd.filesize, bd.fileid
             FROM tb_DA006WFILE bd
             WHERE bd.custcd = h.custcd
               AND bd.spjangcd = h.spjangcd
               AND bd.reqdate = h.reqdate
               AND bd.reqnum = h.reqnum
             ORDER BY bd.indatem DESC
             FOR JSON PATH
         ) AS hd_files
        FROM TB_DA006W h
        LEFT JOIN summary s
          ON h.reqdate = s.reqdate AND h.reqnum = s.reqnum
        LEFT JOIN aggregated a
          ON h.reqdate = a.reqdate AND h.reqnum = a.reqnum
        LEFT JOIN material m
          ON h.modeltxt = m.Code
        where h.spjangcd =:spjangcd
        AND h.reqdate BETWEEN :start AND :end
        """;
    if (ordflag != null && !ordflag.isEmpty()) {
      sql += " and h.ordflag = :ordflag ";
      param.addValue("ordflag", "%" + ordflag + "%");
    }
    if (compcd != null && !ordflag.isEmpty()) {
      sql += " and h.compcd like :compcd ";
      param.addValue("compcd", "%" + compcd + "%");
    }

    return sqlRunner.getRows(sql, param);
  }

  public List<Map<String, Object>> download(Map<String, Object> reqnum) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    StringBuilder sql = new StringBuilder();
    dicParam.addValue("reqnum", reqnum.get("reqnum"));

    sql.append("""
                select
                        filepath,
                        reqdate,
                        filesvnm,
                        fileornm
                from tb_DA006WFILE
                where
                    reqnum = :reqnum
                """);
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
    return items;
  }
}
