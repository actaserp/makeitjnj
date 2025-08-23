package mes.app.request.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_DA006WFile;
import mes.domain.model.CopyResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RequestService {

  @Autowired
  SqlRunner sqlRunner;

  @Autowired
  TB_DA006WFILERepository tbDa006WFILERepository;

  @Autowired
  private TB_DA006WRepository tbDa006WRepository;

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

  public List<Map<String, Object>> getTab2Read(Integer compcd ,String company_name, String ordflag, Timestamp start, Timestamp end, String spjangcd) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("compcd", compcd);
    param.addValue("company_name", company_name);
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
             sc.Value as ordflag,
            h.cltcd,
            h.cltnm,
            h.indate,
            h.modeltxt AS model_naem,
           h.pcode as model_code,
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
        LEFT JOIN summary s ON h.reqdate = s.reqdate AND h.reqnum = s.reqnum
        LEFT JOIN aggregated a ON h.reqdate = a.reqdate AND h.reqnum = a.reqnum
        LEFT JOIN latest_model_history mh ON h.pcode = mh.modelid
        left join sys_code sc on sc.Code = ordflag and CodeType ='ordflag'
        WHERE h.spjangcd = :spjangcd
          AND h.reqdate BETWEEN :start AND :end
        """;
    if (ordflag != null && !ordflag.isEmpty()) {
      sql += " and h.ordflag = :ordflag ";
      param.addValue("ordflag", ordflag );
    }
    if (compcd != null) {
      sql += " and h.cltcd like :compcd ";
      param.addValue("compcd", "%" + compcd + "%");
    }

    if (company_name != null && !company_name.isEmpty()) {
      sql += " and h.cltnm like :company_name ";
      param.addValue("company_name", "%" + company_name + "%");
    }

     sql +="""
         order by h.reqdate desc
        """;

//    log.info("getTab2Read  SQL: {}", sql);
//    log.info("SQL Parameters: {}", param.getValues());
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
             h.deldate ,
          h.reqnum,
          h.pcode as model_code,
          h.modeltxt as model_name,
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
          d.pcode as item_code,
          d.pname,
          d.modelnm,
          d.jobflag,
          d.qty,
          d.setamt,
          d.saleamt,
          d.uamt,
          d.remark AS detail_remark,
          d.clttype, 
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
          ON h.pcode = mh.modelid
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


  public Map<String, Object> getOrderMailDeta(String reqnum) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("reqnum", reqnum);

    String sql = """
        select
        projectno ,
        reqdate ,
        cltnm,
        perid ,
        modeltxt ,
        setsamt,
        setqty ,
        amount
        from tb_da006w
        where reqnum = :reqnum
        """;

    return this.sqlRunner.getRow(sql, paramMap);
  }

  public int SaveUnitPrice(Integer pcode, String pname, String puamt, String cltcd, String inputDate) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("pcode", pcode);
    param.addValue("pname", pname != null ? pname.trim() : null);
    param.addValue("psize", null);
    param.addValue("puamt", new BigDecimal(puamt.replaceAll(",", "")));
    param.addValue("inputdate", inputDate);
    param.addValue("cltcd", cltcd);

    String sql = """
        MERGE INTO mat_uamt AS target
        USING (
            SELECT :pcode AS PCODE,
                   :pname AS PNAME,
                   :psize AS PSIZE,
                   :puamt AS PUAMT,
                   :inputdate AS INPUTDATE,
                   :cltcd AS CLTCD
        ) AS source
        ON target.PCODE = source.PCODE
           AND target.PNAME = source.PNAME
           AND target.CLTCD = source.CLTCD
        WHEN MATCHED AND (
            (target.PUAMT IS NULL AND source.PUAMT IS NOT NULL) OR
            (target.PUAMT IS NOT NULL AND source.PUAMT IS NULL) OR
            (target.PUAMT <> source.PUAMT)
        )
        THEN UPDATE SET
            PUAMT = source.PUAMT,
            INPUTDATE = source.INPUTDATE,
            CLTCD = source.CLTCD
        WHEN NOT MATCHED THEN
        INSERT (PCODE, PNAME, PSIZE, PUAMT, INPUTDATE, CLTCD)
        VALUES (source.PCODE, source.PNAME, source.PSIZE, source.PUAMT, source.INPUTDATE, source.CLTCD);
    """;

    int affected = sqlRunner.execute(sql, param);
    if (affected == 0) {
      // 동일 데이터 존재 여부 확인 → 존재하면 성공으로 간주
      String checkSql = """
            SELECT COUNT(1)
            FROM mat_uamt
            WHERE PCODE=:pcode AND PNAME=:pname AND CLTCD=:cltcd AND PUAMT=:puamt
        """;
      int sameCount = sqlRunner.queryForCount(checkSql, param);
      if (sameCount > 0) return 1; // 무변경(no-op)도 성공 처리
    }
    return affected;
  }

  public List<Map<String, Object>> getCopyList(String reqnums) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("reqnums", reqnums);

    String sql= """
        select
        h.*,
        d.*
        from TB_DA006W h
        left join TB_DA007W d on h.reqnum = d.reqnum\s
        where h.reqnum =:reqnum;
        """;

    return sqlRunner.getRows(sql, param);
  }

  @Transactional
  public CopyResult copyOrdersSequential(List<String> oldReqnums,
                                         String spjangcd,
                                         String overrideReqdate, // "YYYYMMDD" or null
                                         String actor) {

    int copied = 0, skipped = 0;
    List<String> newReqnums = new ArrayList<>();
    List<Map<String, Object>> failures = new ArrayList<>();

    // 날짜 유틸
    DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    // 1) 단건 조회: 원본 헤더 reqdate 얻기
    final String SQL_SELECT_HEADER_REQDATE = """
        SELECT h.reqdate
        FROM ELV_JNJ.dbo.TB_DA006W h
        WHERE h.spjangcd = :spjangcd
          AND h.reqnum   = :oldReqnum
        """;

    // 2) 헤더 복제 (reqdate/reqnum/deldate 오버라이드)
    final String SQL_INSERT_HEADER = """
    INSERT INTO ELV_JNJ.dbo.TB_DA006W (
        custcd, spjangcd, reqdate, reqnum, cltcd, cltnm, saupnum, cltzipcd, cltaddr, cltaddr02,
        delzipcd, deladdr, deldate, perid, divicd, domcls, moncls, monrate, remark, operid,
        dperid, sperid, ordflag, egrb, modeltxt, setsamt, setqty, amount, outamt, eyunamt,
        pereyunamt, eyunyul, toteyunamt, projectno, indate, inperid, telno, adflag, userflag, pcode
    )
    SELECT
        h.custcd,
        h.spjangcd,
        :newReqdate,        -- ★ override
        :newReqnum,         -- ★ override
        h.cltcd, h.cltnm, h.saupnum, h.cltzipcd, h.cltaddr, h.cltaddr02,
        h.delzipcd, h.deladdr,
        :newDeldate,        -- ★ override
        h.perid, h.divicd, h.domcls, h.moncls, h.monrate, h.remark, h.operid,
        h.dperid, h.sperid,
        0 AS ordflag,       -- ★ 신규 복사건은 무조건 0
        h.egrb, h.modeltxt, h.setsamt, h.setqty, h.amount, h.outamt, h.eyunamt,
        h.pereyunamt, h.eyunyul, h.toteyunamt, h.projectno,
        h.indate,           -- 정책에 따라 바꿔도 됨
        h.inperid,          -- 정책에 따라 actor로 바꿔도 됨
        h.telno, h.adflag, h.userflag, h.pcode
    FROM ELV_JNJ.dbo.TB_DA006W h
    WHERE h.spjangcd = :spjangcd
      AND h.reqnum   = :oldReqnum
    """;

    // 3) 디테일 복제 (reqdate/reqnum 오버라이드)
    final String SQL_INSERT_DETAIL = """
        INSERT INTO ELV_JNJ.dbo.TB_DA007W (
            custcd, spjangcd, reqdate, reqnum, reqseq,
            pcode, modelnm, japcode, pname, jobflag,
            setamt, saleamt, qty, uamt, uamttxt,
            remark, indate, inperid, ordtext, stframedv, stexplydv, clttype
        )
        SELECT
            d.custcd,
            d.spjangcd,
            :newReqdate,        -- ★ override
            :newReqnum,         -- ★ override
            d.reqseq,
            d.pcode, d.modelnm, d.japcode, d.pname, d.jobflag,
            d.setamt, d.saleamt, d.qty, d.uamt, d.uamttxt,
            d.remark, d.indate, d.inperid, d.ordtext, d.stframedv, d.stexplydv, d.clttype
        FROM ELV_JNJ.dbo.TB_DA007W d
        WHERE d.spjangcd = :spjangcd
          AND d.reqnum   = :oldReqnum
        """;

    for (String oldReqnum : oldReqnums) {
      try {
        // A) 신규 주문번호 채번 (spjangcd 기준, 동시성 안전 구현 권장)
        String newReqnum = tbDa006WRepository.getNextReqnum(spjangcd);

        // B) 사용할 reqdate 결정
        String baseReqdate = overrideReqdate;
        if (!StringUtils.hasText(baseReqdate)) {
          MapSqlParameterSource p = new MapSqlParameterSource()
              .addValue("spjangcd", spjangcd)
              .addValue("oldReqnum", oldReqnum);
          List<Map<String, Object>> rows = sqlRunner.getRows(SQL_SELECT_HEADER_REQDATE, p);
          if (rows.isEmpty() || rows.get(0).get("reqdate") == null) {
            // 원본이 없으면 스킵
            skipped++;
            failures.add(Map.of("oldReqnum", oldReqnum, "reason", "header not found"));
            continue;
          }
          baseReqdate = String.valueOf(rows.get(0).get("reqdate")).replaceAll("[^0-9]", "");
        } else {
          baseReqdate = overrideReqdate.replaceAll("[^0-9]", "");
        }

        // C) deldate = reqdate + 5일
        String deldate = LocalDate.parse(baseReqdate, BASIC).plusDays(5).format(BASIC);

        // D) 공통 파라미터
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("spjangcd", spjangcd)
            .addValue("oldReqnum", oldReqnum)
            .addValue("newReqnum", newReqnum)
            .addValue("newReqdate", baseReqdate)
            .addValue("newDeldate", deldate)
            .addValue("actor", actor);

        // E) 헤더/디테일 INSERT … SELECT
        int h = sqlRunner.execute(SQL_INSERT_HEADER, param);
        int d = sqlRunner.execute(SQL_INSERT_DETAIL, param);

        if (h > 0) {
          copied++;
          newReqnums.add(newReqnum);
        } else {
          skipped++;
          failures.add(Map.of(
              "oldReqnum", oldReqnum,
              "reason", "header insert affected 0 rows"
          ));
        }
      } catch (Exception ex) {
        skipped++;
        failures.add(Map.of(
            "oldReqnum", oldReqnum,
            "reason", ex.getMessage()
        ));
        // 부분성공 허용. 전체 원자성을 원하면 throw 해서 롤백.
      }
    }

    return new CopyResult(copied, skipped, newReqnums, failures);
  }


  /*public int SaveUnitPrice(Integer pcode, String pname, String puamt, String cltcd, String inputDate) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("pcode", pcode);
    param.addValue("pname", pname);
    param.addValue("psize", null); // 빈 문자열 대신 null
    param.addValue("puamt", new BigDecimal(puamt.replaceAll(",", ""))); // 숫자로 변환
    param.addValue("inputdate", inputDate);
    param.addValue("cltcd", cltcd); // 무조건 바인딩

    StringBuilder sql = new StringBuilder();
    sql.append("""
        MERGE INTO mat_uamt AS target
        USING (
            SELECT :pcode AS PCODE,
                   :pname AS PNAME,
                   :psize AS PSIZE,
                   :puamt AS PUAMT,
                   :inputdate AS INPUTDATE,
                   :cltcd AS CLTCD
        ) AS source
        ON target.PCODE = source.PCODE
           AND target.PNAME = source.PNAME
           AND target.CLTCD = source.CLTCD
        WHEN MATCHED AND (
            (target.PUAMT IS NULL AND source.PUAMT IS NOT NULL) OR
            (target.PUAMT IS NOT NULL AND source.PUAMT IS NULL) OR
            (target.PUAMT <> source.PUAMT)
        )
        THEN UPDATE SET
            PUAMT = source.PUAMT,
            INPUTDATE = source.INPUTDATE,
            CLTCD = source.CLTCD
        WHEN NOT MATCHED THEN
        INSERT (PCODE, PNAME, PSIZE, PUAMT, INPUTDATE, CLTCD)
        VALUES (source.PCODE, source.PNAME, source.PSIZE, source.PUAMT, source.INPUTDATE, source.CLTCD);
    """);

    log.info("주문등록 단가 저장 SQL: {}", sql);
    log.info("SQL Parameters: {}", param.getValues());

    return sqlRunner.execute(sql.toString(), param);
  }*/

}
