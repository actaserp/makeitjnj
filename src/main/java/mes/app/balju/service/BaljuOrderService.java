package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BaljuOrderService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getBaljuList(String date_kind, Timestamp start, Timestamp end, String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("date_kind", date_kind);
    dicParam.addValue("start", start);
    dicParam.addValue("end", end);
    dicParam.addValue("spjangcd", spjangcd);

    String sql = """
        WITH base_data AS (
            SELECT
                bh.id AS bh_id,
                bh.Company_id,
                bh.JumunDate,
                bh.JumunNumber,
                CASE bh.SujuType
                    WHEN 'Urgent' THEN '긴급'
                    WHEN 'Irregular' THEN '수시'
                    WHEN 'Regular' THEN '정기'
                    ELSE '기타'
                END AS BaljuTypeName,
                bh.SujuType,
                bh.State,
                bh.DeliveryDate,
                bh.ShipmentState,
                bh.spjangcd,
                b.CompanyName,
                b.BaljuHead_id,
                b.Material_id,
                b.SujuQty,
                b.UnitPrice,
                b.Price,
                b.Vat,
                b.TotalAmount,
                b.Description,
                b.State AS balju_state,
                b.ShipmentState AS balju_shipment_state,
                m.Code AS product_code,
                m.Name AS product_name,
                u.Name AS unit,
                ISNULL(mi.SujuQty2, 0) AS SujuQty2,
                CASE 
                    WHEN b.SujuQty - ISNULL(mi.SujuQty2, 0) > 0 THEN b.SujuQty - ISNULL(mi.SujuQty2, 0)
                    ELSE 0
                END AS SujuQty3,
                ROW_NUMBER() OVER (PARTITION BY bh.JumunNumber ORDER BY b.id ASC) AS rn,
                (
                    SELECT
                      CASE
                        WHEN SUM(CASE WHEN b2.State = 'received' THEN 1 ELSE 0 END) = COUNT(*) THEN 'received'
                        WHEN SUM(CASE WHEN b2.State = 'draft' THEN 1 ELSE 0 END) = COUNT(*) THEN 'draft'
                        WHEN SUM(CASE WHEN b2.State = 'canceled' THEN 1 ELSE 0 END) = COUNT(*) THEN 'canceled'
                        ELSE 'partial'
                      END
                    FROM balju b2
                    WHERE b2.BaljuHead_id = bh.id
                  ) AS BalJuHeadType
            FROM balju_head bh
            LEFT JOIN balju b 
                ON b.BaljuHead_id = bh.id 
                AND b.spjangcd = bh.spjangcd 
                AND b.JumunNumber = bh.JumunNumber
            LEFT JOIN material m ON m.id = b.Material_id AND m.spjangcd = b.spjangcd
            LEFT JOIN unit u ON m.Unit_id = u.id AND u.spjangcd = b.spjangcd
            LEFT JOIN (
                SELECT SourceDataPk, SUM(InputQty) AS SujuQty2
                FROM mat_inout
                WHERE SourceTableName = 'balju' AND ISNULL(_status, 'a') = 'a'
                GROUP BY SourceDataPk
            ) mi ON mi.SourceDataPk = b.id
            WHERE bh.spjangcd = :spjangcd
        """;

    if (date_kind.equals("sales")) {
      sql += " AND bh.JumunDate BETWEEN :start AND :end ";
    } else {
      sql += " AND bh.DeliveryDate BETWEEN :start AND :end ";
    }

    sql += """
        )
        SELECT
            bh_id,
            JumunNumber,
            MAX(Company_id) AS Company_id,
            MAX(CompanyName) AS CompanyName,
            MAX(BaljuHead_id) AS BaljuHead_id,
            MAX(JumunDate) AS JumunDate,
              MAX(BaljuTypeName) AS BaljuTypeName,
            MAX(CASE WHEN rn = 1 THEN product_code END) AS product_code,
            MAX(CASE WHEN rn = 1 THEN product_name END) AS product_name,
            MAX(CASE WHEN rn = 1 THEN unit END) AS unit,
            SUM(SujuQty) AS SujuQty,
            SUM(UnitPrice) AS BaljuUnitPrice,
            SUM(Price) AS BaljuPrice,
            SUM(Vat) AS BaljuVat,
            SUM(TotalAmount) AS BaljuTotalPrice,
            MAX(State) AS StateName,
            MAX(BalJuHeadType) AS BalJuHeadType,
            SUM(SujuQty2) AS SujuQty2,
            SUM(SujuQty3) AS SujuQty3,
            MAX(ShipmentState) AS ShipmentStateName,
            MAX(DeliveryDate) AS DueDate,
            MAX(Description) AS Description
        FROM base_data
        GROUP BY JumunNumber, bh_id
        ORDER BY MAX(DeliveryDate) DESC, bh_id
        """;

//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
return this.sqlRunner.getRows(sql, dicParam);
}

  public Map<String, Object> getBaljuDetail(int id) {
  MapSqlParameterSource paramMap = new MapSqlParameterSource();
  paramMap.addValue("id", id);

    String sql = """
        WITH balju_total AS (
            SELECT
                [BaljuHead_id] AS bh_id,
                SUM(ISNULL([TotalAmount], 0)) AS total_amount_sum
            FROM balju
            GROUP BY [BaljuHead_id]
        )
        SELECT
            bh.id AS bh_id,
            bh.[Company_id],
            c.[Name] AS CompanyName,
            bh.[JumunDate],
            bh.[DeliveryDate],
            bh.special_note,
            bh.[JumunNumber],
        
            b.id AS balju_id,
            b.[Material_id],
            ISNULL(m.[Code], '') AS product_code,
            ISNULL(m.[Name], '') AS product_name,
            ISNULL(mg.[Name], '') AS MaterialGroupName,
            ISNULL(mg.id, 0) AS MaterialGroup_id,
            dbo.fn_code_name('mat_type', mg.[MaterialType]) AS MaterialTypeName,
            s.[Value] AS BaljuTypeName,
            b.[SujuQty],
            u.[Name] AS unit,
            b.[UnitPrice] AS BaljuUnitPrice,
            b.[Price] AS BaljuPrice,
            b.[Vat] AS BaljuVat,
            b.[InVatYN],
            b.[TotalAmount] AS LineTotalAmount,
            ISNULL(bt.total_amount_sum, 0) AS BaljuTotalPrice,
        
            CONVERT(varchar, b.[ProductionPlanDate], 23) AS production_plan_date,
            CONVERT(varchar, b.[ShipmentPlanDate], 23) AS shiment_plan_date,
            b.[Description],
            b.[AvailableStock],
            b.[ReservationStock],
            mi.SujuQty2,
        
            -- BaljuHead 상태 계산
            (
                SELECT
                    CASE
                        WHEN SUM(CASE WHEN b2.[State] = 'received' THEN 1 ELSE 0 END) = COUNT(*) THEN 'received'
                        WHEN SUM(CASE WHEN b2.[State] = 'draft' THEN 1 ELSE 0 END) = COUNT(*) THEN 'draft'
                        WHEN SUM(CASE WHEN b2.[State] = 'canceled' THEN 1 ELSE 0 END) = COUNT(*) THEN 'canceled'
                        ELSE 'partial'
                    END
                FROM balju b2
                WHERE b2.[BaljuHead_id] = bh.id
            ) AS BalJuHeadType,
        
            -- 상태명 코드
            dbo.fn_code_name(
                'balju_state',
                (
                    SELECT
                        CASE
                            WHEN SUM(CASE WHEN b2.[State] = 'received' THEN 1 ELSE 0 END) = COUNT(*) THEN 'received'
                            WHEN SUM(CASE WHEN b2.[State] = 'draft' THEN 1 ELSE 0 END) = COUNT(*) THEN 'draft'
                            WHEN SUM(CASE WHEN b2.[State] = 'canceled' THEN 1 ELSE 0 END) = COUNT(*) THEN 'canceled'
                            ELSE 'partial'
                        END
                    FROM balju b2
                    WHERE b2.[BaljuHead_id] = bh.id
                )
            ) AS bh_StateName,
        
            b.[State] AS BalJuType,
            dbo.fn_code_name('balju_state', b.[State]) AS balju_StateName,
            CONVERT(varchar, b.[_created], 23) AS create_date
        
        FROM balju_head bh
        LEFT JOIN balju b ON b.[BaljuHead_id] = bh.id
        LEFT JOIN material m ON m.id = b.[Material_id] AND m.spjangcd = b.spjangcd
        LEFT JOIN mat_grp mg ON mg.id = m.[MaterialGroup_id] AND mg.spjangcd = b.spjangcd
        LEFT JOIN unit u ON m.[Unit_id] = u.id AND u.spjangcd = b.spjangcd
        LEFT JOIN company c ON c.id = b.[Company_id]
        LEFT JOIN sys_code s ON bh.[SujuType] = s.[Code] AND s.[CodeType] = 'Balju_type'
        LEFT JOIN (
            SELECT [SourceDataPk], SUM([InputQty]) AS SujuQty2
            FROM mat_inout
            WHERE [SourceTableName] = 'balju' AND ISNULL([_status], 'a') = 'a'
            GROUP BY [SourceDataPk]
        ) mi ON mi.[SourceDataPk] = b.id
        LEFT JOIN balju_total bt ON bt.bh_id = bh.id
        WHERE bh.id = :id
        """;
//    log.info("발주상세 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);

    if (rows.isEmpty()) return Collections.emptyMap();

    // 공통 헤더 정보 (첫 번째 row 기준)
    Map<String, Object> header = new LinkedHashMap<>();
    Map<String, Object> first = rows.get(0);

    header.put("mode", "edit");
    header.put("id", first.get("bh_id"));
    header.put("Company_id", first.get("Company_id"));
    header.put("CompanyName", first.get("CompanyName"));
    header.put("JumunDate", first.get("JumunDate"));
    header.put("DeliveryDate", first.get("DeliveryDate"));
    header.put("State", first.get("BalJuHeadType"));
    header.put("StateName", first.get("bh_StateName"));
    header.put("special_note", first.get("special_note"));
    header.put("JumunNumber", first.get("JumunNumber"));

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();

      item.put("id", row.get("balju_id"));
      item.put("Material_id", row.get("Material_id"));
      item.put("product_code", row.get("product_code"));
      item.put("product_name", row.get("product_name"));
      item.put("quantity", row.get("SujuQty"));
      item.put("unit_price", row.get("BaljuUnitPrice"));
      item.put("supply_price", row.get("BaljuPrice"));
      item.put("vat", row.get("BaljuVat"));
      item.put("total_price", row.get("LineTotalAmount"));
      item.put("description", row.get("Description"));
      item.put("vatIncluded", row.get("InVatYN"));
      item.put("State", row.get("BalJuType"));
      item.put("balju_StateName", row.get("balju_StateName"));

      items.add(item);
    }

    header.put("items", items);
    return header;
  }

  //주문 번호 생성
  @Transactional
  public String makeJumunNumber(Date dataDate) {
    String baseDate = new SimpleDateFormat("yyyyMMdd").format(dataDate);

    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("data_date", baseDate);
    paramMap.addValue("code", "BaljuNumber");

    int currVal = 1;

    // 1. 현재 값 조회
    String checkSql = """
            SELECT "CurrVal" 
            FROM seq_maker 
            WHERE "Code" = :code AND "BaseDate" = :data_date
            FOR UPDATE
        """;
    Map<String, Object> mapRow = sqlRunner.getRow(checkSql, paramMap);

    if (mapRow != null && mapRow.containsKey("CurrVal")) {
      currVal = (int) mapRow.get("CurrVal") + 1;

      // 2. 시퀀스 업데이트
      String updateSql = """
              UPDATE seq_maker 
              SET "CurrVal" = :currVal, "_modified" = now()
              WHERE "Code" = :code AND "BaseDate" = :data_date
          """;
      paramMap.addValue("currVal", currVal);
      sqlRunner.execute(updateSql, paramMap);

    } else {
      // 3. 신규 row 생성
      currVal = 1;

      String insertSql = """
              INSERT INTO seq_maker("Code", "BaseDate", "Code2", "CurrVal", "_modified") 
              VALUES (:code, :data_date, NULL, :currVal, now())
          """;
      paramMap.addValue("currVal", currVal);
      sqlRunner.execute(insertSql, paramMap);
    }

    // 4. 주문번호 조립
    String jumunNumber = baseDate + "-" + String.format("%04d", currVal);
    //log.info(" 최종 생성된 주문번호: {}", jumunNumber);
    return jumunNumber;
  }

  public List<Map<String, Object>> getBaljuPrice(int pcode,  String pname, String cltcd) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("pcode", pcode);
    dicParam.addValue("pname", pname);
    dicParam.addValue("cltcd", cltcd);

    String sql = """
        SELECT TOP 1 PUAMT
              FROM mat_uamt 
              WHERE PNAME = :pname
                AND PCODE = :pcode
        """;
    if (cltcd != null && !cltcd.isEmpty()) {
      sql+= """ 
          AND cltcd = :cltcd 
          """;
      dicParam.addValue("cltcd", cltcd);
    }

    sql+= """
         ORDER BY INPUTDATE DESC
        """;

//    log.info("발주 단가 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
    return items;
  }

  public void updateMatCompUnitPrice(int materialId, int companyId, String jumunDate, double newUnitPrice, String changerName) {
    String sql = """
            UPDATE mat_comp_uprice
            SET "FormerUnitPrice" = "UnitPrice",
                "UnitPrice" = :unitPrice,
                "ChangeDate" = now(),
                "ChangerName" = :changerName
            WHERE "Material_id" = :materialId
              AND "Company_id" = :companyId
              AND TO_DATE(:jumunDate, 'YYYY-MM-DD') BETWEEN "ApplyStartDate" AND "ApplyEndDate"
              AND "Type" = '01'
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("unitPrice", newUnitPrice)
        .addValue("changerName", changerName)
        .addValue("materialId", materialId)
        .addValue("companyId", companyId)
        .addValue("jumunDate", jumunDate);

    int affected = sqlRunner.execute(sql, params);
    //log.info("🔁 단가 업데이트 완료 (이전 단가 백업 포함): {}건", affected);
  }

  public List<Map<String, Object>> balju_stop(Integer id) {
    // 1. 현재 상태 조회 (입고 수량 포함)
    String selectSql = """
            SELECT b."State", b."SujuQty", mi."SujuQty2"
            FROM balju b
            LEFT JOIN (
                SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
                FROM mat_inout
                WHERE "SourceTableName" = 'balju' AND COALESCE("_status", 'a') = 'a'
                GROUP BY "SourceDataPk"
            ) mi ON mi."SourceDataPk" = b.id
            WHERE b.id = :id
        """;

    MapSqlParameterSource selectParams = new MapSqlParameterSource().addValue("id", id);

    Map<String, Object> result = sqlRunner.queryForObject(selectSql, selectParams, (rs, rowNum) -> {
      Map<String, Object> map = new HashMap<>();
      map.put("State", rs.getString("State"));
      map.put("SujuQty", rs.getInt("SujuQty"));

      // SujuQty2는 null 가능 → 안전하게 처리
      int sujuQty2 = rs.getInt("SujuQty2");
      if (rs.wasNull()) sujuQty2 = 0;
      map.put("SujuQty2", sujuQty2);

      return map;
    });

    // 2. 현재 값 추출
    String currentState = (String) result.get("State");
    int sujuQty = (int) result.get("SujuQty");
    int sujuQty2 = (int) result.get("SujuQty2");

    // 3. 새 상태값 결정
    String newState;
    if ("canceled".equalsIgnoreCase(currentState)) {
      // 중지 취소 → 입고량에 따라 상태 판단
      if (sujuQty2 == 0) {
        newState = "draft";
      } else if (sujuQty2 < sujuQty) {
        newState = "partial";
      } else {
        newState = "received";
      }
    } else {
      // 중지가 아니면 → 중지 처리
      newState = "canceled";
    }

    // 4. 상태 업데이트
    String updateSql = """
            UPDATE balju
            SET "State" = :state
            WHERE id = :id
        """;

    MapSqlParameterSource updateParams = new MapSqlParameterSource()
        .addValue("state", newState)
        .addValue("id", id);

    int affected = sqlRunner.execute(updateSql, updateParams);

    // 5. 결과 반환
    return List.of(Map.of(
        "updatedRows", affected,
        "newState", newState
    ));
  }

  public int saveCompanyUnitPrice(Map<String, Object> data) {
    Integer materialId = CommonUtil.tryIntNull(data.get("Material_id"));
    Integer companyId = CommonUtil.tryIntNull(data.get("Company_id"));
    String product_name = CommonUtil.tryString(data.get("Product_name"));
    String supply_price = CommonUtil.tryString(data.get("Supply_price"));
    String inputDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("pcode", materialId);
    param.addValue("pname", product_name);
    param.addValue("psize", null); // 빈 문자열 대신 null
    param.addValue("puamt", new BigDecimal(supply_price.replaceAll(",", ""))); // 숫자로 변환
    param.addValue("inputdate", inputDate);
    param.addValue("cltcd", companyId); // 무조건 바인딩

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

//    log.info("주문등록 단가 저장 SQL: {}", sql);
//    log.info("SQL Parameters: {}", param.getValues());

    return sqlRunner.execute(sql.toString(), param);
  }


  /*public int saveCompanyUnitPrice(Map<String, Object> data) {
    Integer materialId = CommonUtil.tryIntNull(data.get("Material_id"));
    Integer companyId = CommonUtil.tryIntNull(data.get("Company_id"));

    // ApplyStartDate 처리
    String applyStartDateStr = CommonUtil.tryString(data.get("ApplyStartDate"));
    LocalDateTime applyStartDateLocal = LocalDateTime.parse(applyStartDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    Timestamp applyStartDate = Timestamp.valueOf(applyStartDateLocal);

    // 현재 날짜와 비교하여 ApplyEndDate 설정
    LocalDate applyStartDateDate = applyStartDateLocal.toLocalDate();
    LocalDate today = LocalDate.now();

    Timestamp applyEndDate = applyStartDateDate.equals(today)
        ? applyStartDate
        : Timestamp.valueOf(applyStartDateDate.minusDays(1).atStartOfDay());

    Timestamp applyEndDate2 = CommonUtil.tryTimestamp("2100-12-31");

    Float unitPrice = CommonUtil.tryFloatNull(data.get("UnitPrice"));
    String changerName = CommonUtil.tryString(data.get("ChangerName"));
    String type = CommonUtil.tryString(data.get("type"));
    Integer userId = CommonUtil.tryIntNull(data.get("user_id"));

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("materialId", materialId);
    dicParam.addValue("companyId", companyId);
    dicParam.addValue("applyStartDate", applyStartDate, java.sql.Types.TIMESTAMP);
    dicParam.addValue("applyEndDate", applyEndDate, java.sql.Types.TIMESTAMP);
    dicParam.addValue("applyEndDate2", applyEndDate2, java.sql.Types.TIMESTAMP);
    dicParam.addValue("unitPrice", unitPrice);
    dicParam.addValue("changerName", changerName);
    dicParam.addValue("userId", userId);
    dicParam.addValue("type", type);
    dicParam.addValue("formerUnitPrice", null);

    String sql = """
        select id, "UnitPrice"
        from mat_comp_uprice
        where "Material_id" = :materialId
        and "Company_id" = :companyId
        and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
        """;

    Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
    if (!MapUtils.isEmpty(item)) {
      dicParam.addValue("formerUnitPrice", CommonUtil.tryFloatNull(item.get("UnitPrice")));
    }

    sql = """
        update mat_comp_uprice
        set "ApplyEndDate" = :applyEndDate
        where "Material_id" = :materialId
        and "Company_id" = :companyId
        and :applyStartDate between "ApplyStartDate" and "ApplyEndDate"
        """;

    this.sqlRunner.execute(sql, dicParam);

    sql = """
        INSERT INTO public.mat_comp_uprice
        ("_created", "_creater_id", "Material_id", "Company_id", "ApplyStartDate", 
         "ApplyEndDate", "UnitPrice", "FormerUnitPrice", "ChangeDate", "ChangerName", "Type")
        VALUES (
         now(), :userId, :materialId, :companyId, :applyStartDate,
         :applyEndDate2, :unitPrice, :formerUnitPrice, now(), :changerName, :type
        )
        """;

    return this.sqlRunner.execute(sql, dicParam);
  }*/

  //FROM 데이터 조회용
  public Map<String, Object> getSenderInfo(String userid) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("userid", userid);

    String sql = """
        select
        au.spjangcd ,
        x.spjangnm ,
        x.tel1 ,
        x.adresa
        from auth_user au 
        left join tb_xa012 x on x.spjangcd = au.spjangcd 
        where au.username =:userid
        """;
//    log.info("FROM (발신자) SQL: {}", sql);
//    log.info("FROM (발신자)데이터: {}", paramMap.getValues());
    return this.sqlRunner.getRow(sql, paramMap);
  }

  public Map<String, Object> getReceiverInfo(Integer companyId) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("companyId", companyId);

    String sql = """
        SELECT
            c."Name" AS company_name,
            c."TelNumber"  AS tel,
            c."Address" AS address
        FROM company c
        WHERE c.id = :companyId 
        """;
//    log.info("TO (수신처) SQL: {}", sql);
//    log.info("TO (수신처) 데이터: {}", paramMap.getValues());
    return this.sqlRunner.getRow(sql, paramMap);
  }

  public String getReceiverEmail(Integer bhId) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("bhId", bhId);

    String sql = """
         SELECT c.Email
        from company c
        WHERE c.id = :bhId
        """;

    return this.sqlRunner.queryForObject(sql, param, (rs, rowNum) -> rs.getString("Email"));
  }

}
