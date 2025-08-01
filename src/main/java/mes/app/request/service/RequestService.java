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
  public List<Map<String, Object>> getBomComponentTreeList(Integer bomId, String spjangcd) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("bomId", bomId);
    param.addValue("spjangcd", spjangcd);
    String sql = """
        SELECT
            b.id AS bom_id,
            b.Name AS bom_name,
            b.Material_id AS mat_id,
            pm.Code AS mat_code,
            pm.Name AS mat_name,
            NULL AS parent_key,
            NULL AS parent_mat_id,
            1 AS lvl,
            NULL AS quantity,
            b.OutputAmount AS produced_qty,
            1.0 AS bom_ratio,
            NULL AS bom_qty,
            NULL AS unit,
            NULL AS Description,
            NULL AS bc_id,
            NULL AS tot_order,
            '모품목' AS part_type,
            b.Version,
            b.StartDate,
            b.EndDate
        FROM bom b
        LEFT JOIN material pm ON pm.id = b.Material_id
        WHERE b.spjangcd = :spjangcd
          AND b.id = :bomId
        UNION ALL
        SELECT
            b.id AS bom_id,
            b.Name AS bom_name,
            bc.Material_id AS mat_id,
            cm.Code AS mat_code,
            cm.Name AS mat_name,
            CAST(b.Material_id AS VARCHAR) AS parent_key,
            b.Material_id AS parent_mat_id,
            2 AS lvl,
            bc.Amount AS quantity,
            b.OutputAmount AS produced_qty,
            CAST(bc.Amount / NULLIF(b.OutputAmount, 0) AS FLOAT) AS bom_ratio,
            CAST(bc.Amount AS VARCHAR) + '/' + CAST(b.OutputAmount AS VARCHAR),
            u.Name AS unit,
            bc.Description,
            bc.id AS bc_id,
            RIGHT(REPLICATE('0', 4) + CAST(bc._order AS VARCHAR), 4) AS tot_order,
            '자품목' AS part_type,
            b.Version,
            b.StartDate,
            b.EndDate
        FROM bom b
        JOIN bom_comp bc ON bc.BOM_id = b.id
        LEFT JOIN material cm ON cm.id = bc.Material_id
        LEFT JOIN unit u ON u.id = cm.Unit_id
        WHERE b.spjangcd = :spjangcd
          AND b.id = :bomId
        ORDER BY part_type DESC, tot_order ASC
        """;
    return sqlRunner.getRows(sql , param);
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
            WHERE rn = 1
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
        ),
        latest_model_history AS (
            SELECT *
            FROM (
                SELECT *,
                       ROW_NUMBER() OVER (PARTITION BY modelid ORDER BY version_no DESC) AS rn
                FROM model_history
            ) t
            WHERE rn = 1
        )
        SELECT
            h.reqnum,
            STUFF(STUFF(h.reqdate, 5, 0, '-'), 8, 0, '-') AS reqdate,
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
            mh.modeltxt_current ,
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
        LEFT JOIN latest_model_history mh
            ON h.modeltxt = mh.modelid
        WHERE h.spjangcd = :spjangcd
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
    log.info("getTab2Read  SQL: {}", sql);
    log.info("SQL Parameters: {}", param.getValues());
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


  public List<Map<String, Object>> getOrderDetail(String reqnum, String formattedReqdate, String custcd, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("reqnum", reqnum);
    paramMap.addValue("reqdate", formattedReqdate);
    paramMap.addValue("custcd", custcd);
    paramMap.addValue("spjangcd", spjangcd);

    String sql = """
        SELECT 
          h.reqdate,
          h.reqnum,
          h.modeltxt as modelcd,
          m.Name as modeltxt,
          m.MaterialGroup_id ,
          m.id as Material_id,
          h.cltnm,
          h.indate,
          h.perid,
          h.telno,
          h.remark AS head_remark,
          h.setsamt,
          h.setqty,
          h.amount,
          h.outamt,
          h.eyunamt,
          h.pereyunamt,
          h.eyunyul,
          h.toteyunamt,
          h.projectno,
          h.cltzipcd,
          h.cltaddr,
          d.reqseq,
          d.pname,
          d.modelnm,
          d.jobflag,
          d.qty,
          d.setamt,
          d.saleamt,
          d.uamt,
          d.remark AS detail_remark,
          mh.version_no,
          mh.prev_modeltxt,
          mh.modeltxt_current,
          mh.change_date,
          mh.changer_name
        FROM TB_DA006W h
        LEFT JOIN material m on h.modeltxt = m.Code 
        LEFT JOIN TB_DA007W d
          ON h.custcd   = d.custcd
         AND h.spjangcd = d.spjangcd
         AND h.reqdate  = d.reqdate
         AND h.reqnum   = d.reqnum
        LEFT JOIN (
          SELECT *
          FROM (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY modelid ORDER BY version_no DESC) AS rn
            FROM model_history
          ) mh
          WHERE rn = 1
        ) mh
          ON h.modeltxt = mh.modelid
         AND h.custcd   = mh.custcd
         AND h.spjangcd = mh.spjangcd
        WHERE h.reqnum = :reqnum
          AND h.reqdate = :reqdate
          AND h.custcd = :custcd
          AND h.spjangcd = :spjangcd
    """;

    log.info("getOrderDetail  SQL: {}", sql);
    log.info("SQL Parameters: {}", paramMap.getValues());
    return this.sqlRunner.getRows(sql, paramMap);
  }


}
