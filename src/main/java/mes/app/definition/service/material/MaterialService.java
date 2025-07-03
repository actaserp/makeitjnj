package mes.app.definition.service.material;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MaterialService {
	
	@Autowired
	SqlRunner sqlRunner;


	public List<Map<String, Object>> getMaterialList(String matType, String matGroupId, String keyword){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("mat_type", matType);
		paramMap.addValue("mat_group_id", matGroupId);
		paramMap.addValue("keyword", keyword);

		String sql = """
				SELECT
						m.id,
						sc.Value as mat_type_name,
						mg.MaterialType AS mat_type,
						mg.Name AS mat_grp_name,
						m.Code AS mat_code,
						m.Name AS mat_name,
						u.Name AS unit_name,
						f.Name AS factory_name,
						m.LotSize AS lot_size,
						m.CustomerBarcode AS customer_barcode,
						m.Thickness AS thickness,
						m.Width AS width,
						m.Length AS length,
						m.Height AS height,
						m.Weight AS weight,
						m.Color AS color,
						m.Usage AS usage,
						m.Class1 AS class1,
						m.Class2 AS class2,
						m.Class3 AS class3,
						m.Standard1 AS starndard1,
						m.Standard2 AS standard2,
						m.Description AS description,
						wc.Name AS workcenter_name,
						e.Name AS equip_name,
						(CAST(m.StandardTime AS VARCHAR) + '(' + m.StandardTimeUnit + ')') AS standard_time,
						m.InputManCount AS input_man_count,
						m.InputManHour AS input_man_hour,
						m.VatExemptionYN AS vat_exempt_yn,
						m.PurchaseOrderStandard AS purchase_order_standard,
						m.LeadTime AS leadtime,
						m.MinOrder AS min_order,
						m.MaxOrder AS max_order,
						sh.Name AS storehouse_name,
						m.StoreHouseLoc AS storehouse_loc,
						m.ManagementLevel AS manage_level,
						m.PackingUnitQty AS packing_unit_qty,
						m.PackingUnitName AS packing_unit_name,
						m.SafetyStock AS safety_stock,
						m.MaxStock AS max_stock,
						m.ProcessSafetyStock AS process_safety_stock,
						m.ValidDays AS valid_days,
						m.InTestYN AS intest_yn,
						m.OutTestYN AS outtest_yn,
						r.Name AS routing_name,
						m.UnitPrice AS unit_price,
						ISNULL(m.LotUseYN, 'N') AS lotUseYn,
						FORMAT(m._created, 'yyyy-MM-dd') AS _created,
						m.Mtyn AS mtyn,
						m.Useyn AS useyn,
						m.Avrqty AS avrqty
				FROM material m
				LEFT JOIN mat_grp mg ON mg.id = m.MaterialGroup_id
				LEFT JOIN unit u ON u.id = m.Unit_id
				LEFT JOIN factory f ON f.id = m.Factory_id
				LEFT JOIN store_house sh ON sh.id = m.StoreHouse_id
				LEFT JOIN work_center wc ON wc.id = m.WorkCenter_id
				LEFT JOIN equ e ON e.id = m.Equipment_id
				LEFT JOIN routing r ON r.id = m.Routing_id
				LEFT JOIN sys_code sc on sc.Code = mg.MaterialType and sc.CodeType = 'mat_type'
				WHERE 1=1
        """;
		if (!StringUtils.isEmpty(matType)) {
			sql += " AND mg.MaterialType = :mat_type ";
		}

		if (!StringUtils.isEmpty(matGroupId)) {
			sql += " AND m.MaterialGroup_id = CAST(:mat_group_id AS INT) ";
		}

		if (!StringUtils.isEmpty(keyword)) {
			sql += """
            AND (
                m.Name LIKE '%' + :keyword + '%'
                OR m.Code LIKE '%' + :keyword + '%'
            )
        """;
		}
		sql += "ORDER BY m.MaterialGroup_id, m.Name ";
		log.info("품목정보 read SQL: {}", sql);
		log.info("SQL Parameters: {}", paramMap.getValues());
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;

	}
	
    public Map<String, Object> getMaterial(int matPK){
    	MapSqlParameterSource paramMap = new MapSqlParameterSource();        
    	paramMap.addValue("mat_pk", matPK);
        
        String sql = """
			select m.id, m."MaterialGroup_id" 
            , mg."Name" as mat_grp_name
            , mg."Code" as mat_grp_code
            , m."Code"
            , m."Name"
            , m."Unit_id"
            , m."Factory_id", m."WorkCenter_id"
            , m."Routing_id" 
            , m."StoreHouse_id"
            , m."StoreHouseLoc"
            , m."PackingUnitQty"
            , m."PackingUnitName"
            , m."SafetyStock" 
            , m."LeadTime", m."LotSize"
            , m."StandardTime", m."StandardTimeUnit" 
            , m."CustomerBarcode"
            , m."Thickness", m."Width", m."Length", m."Height", m."Weight"
            , m."Color"
            , m."Usage" 
            , m."Class1", m."Class2", m."Class3"
            , m."Standard1", m."Standard2" 
            , m."PurchaseOrderStandard", m."ManagementLevel" 
            , m."Description"
            , m."InputManCount", m."InputManHour"
            , m."MaxStock"
            , m."CurrentStock", m."AvailableStock", m."ReservationStock"
            , m."ExpectedOutput"
            , m."VatExemptionYN"
            , m."Equipment_id"
            , m."MinOrder", m."MaxOrder" 
            , m."InTestYN", m."OutTestYN"
            , u."Name" as unit_name
            , m."UnitPrice"
            , m."ProcessSafetyStock" 
            , m."ValidDays" 
            , m."LotUseYN" as "lotUseYn"
            , m."Mtyn" as mtyn
            , m."Useyn" as useyn
            , m."Avrqty" as avrqty
            from material m
            inner join mat_grp mg on m."MaterialGroup_id" = mg.id
            left join unit u on u.id = m."Unit_id"
            where m.id = :mat_pk
        """;
        	
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, paramMap);
        return item;
		
	}

	public int saveMaterial(MultiValueMap<String, Object> data) {
		Integer id = CommonUtil.tryIntNull(data.getFirst("id"));
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("id", id);
		dicParam.addValue("code", CommonUtil.tryString(data.getFirst("Code")));
		dicParam.addValue("name", CommonUtil.tryString(data.getFirst("Name")));
		dicParam.addValue("matGroupId", CommonUtil.tryIntNull(data.getFirst("MaterialGroup_id")));
		dicParam.addValue("unitId", CommonUtil.tryIntNull(data.getFirst("Unit_id")));
		dicParam.addValue("factoryId", CommonUtil.tryIntNull(data.getFirst("Factory_id")));
		dicParam.addValue("workcenterId", CommonUtil.tryIntNull(data.getFirst("WorkCenter_id")));
		dicParam.addValue("equipmentId", CommonUtil.tryIntNull(data.getFirst("Equipment_id")));
		dicParam.addValue("storeHouseId", CommonUtil.tryIntNull(data.getFirst("StoreHouse_id")));
		dicParam.addValue("storeHouseLoc", CommonUtil.tryString(data.getFirst("StoreHouseLoc")));
		dicParam.addValue("managementLevel", CommonUtil.tryString(data.getFirst("ManagementLevel")));
		
		dicParam.addValue("safetyStock", CommonUtil.tryFloatNull(data.getFirst("SafetyStock")));
		dicParam.addValue("maxStock", CommonUtil.tryFloatNull(data.getFirst("MaxStock")));
		dicParam.addValue("processSafetyStock", CommonUtil.tryFloatNull(data.getFirst("ProcessSafetyStock")));
		dicParam.addValue("validDays", Integer.parseInt(data.getFirst("ValidDays").toString()));
		
		if(data.containsKey("lot_use_yn")) {
			dicParam.addValue("lotUseYN", data.getFirst("lot_use_yn").toString());			
		} else {
			dicParam.addValue("lotUseYN", null);
		}

		if(data.containsKey("mtyn")) {
			dicParam.addValue("mtyn", data.getFirst("mtyn").toString());
		} else {
			dicParam.addValue("mtyn", null);
		}

		if(data.containsKey("useyn")) {
			dicParam.addValue("useyn", data.getFirst("useyn").toString());
		} else {
			dicParam.addValue("useyn", null);
		}
		dicParam.addValue("avrqty", CommonUtil.tryString(data.getFirst("avrqty")));

		dicParam.addValue("packingUnitQty", CommonUtil.tryFloatNull(data.getFirst("PackingUnitQty")));
		dicParam.addValue("packingUnitName", CommonUtil.tryString(data.getFirst("PackingUnitName")));
		dicParam.addValue("minOrder", CommonUtil.tryFloatNull(data.getFirst("MinOrder")));
		dicParam.addValue("maxOrder", CommonUtil.tryFloatNull(data.getFirst("MaxOrder")));
		dicParam.addValue("lotSize", CommonUtil.tryFloatNull(data.getFirst("LotSize")));
		dicParam.addValue("leadTime", CommonUtil.tryFloatNull(data.getFirst("LeadTime")));
		
		dicParam.addValue("standardTime", CommonUtil.tryFloatNull(data.getFirst("StandardTime")));
		dicParam.addValue("standardTimeUnit", CommonUtil.tryString(data.getFirst("StandardTimeUnit")));
		dicParam.addValue("thickness", CommonUtil.tryFloatNull(data.getFirst("Thickness")));
		dicParam.addValue("width", CommonUtil.tryFloatNull(data.getFirst("Width")));
		dicParam.addValue("length", CommonUtil.tryFloatNull(data.getFirst("Length")));
		dicParam.addValue("height", CommonUtil.tryFloatNull(data.getFirst("Height")));
		dicParam.addValue("weight", CommonUtil.tryFloatNull(data.getFirst("Weight")));
		dicParam.addValue("color", CommonUtil.tryString(data.getFirst("Color")));
		dicParam.addValue("usage", CommonUtil.tryString(data.getFirst("Usage")));
		dicParam.addValue("class1", CommonUtil.tryString(data.getFirst("Class1")));
		
		dicParam.addValue("class2", CommonUtil.tryString(data.getFirst("Class2")));
		dicParam.addValue("class3", CommonUtil.tryString(data.getFirst("Class3")));
		dicParam.addValue("standard1", CommonUtil.tryString(data.getFirst("Standard1")));
		dicParam.addValue("standard2", CommonUtil.tryString(data.getFirst("Standard2")));
		dicParam.addValue("description", CommonUtil.tryString(data.getFirst("Description")));
		dicParam.addValue("customerBarcode", CommonUtil.tryString(data.getFirst("CustomerBarcode")));
		dicParam.addValue("inputManCount", CommonUtil.tryIntNull(data.getFirst("InputManCount")));
		dicParam.addValue("inputManHour", CommonUtil.tryFloatNull(data.getFirst("InputManHour")));
		dicParam.addValue("purchaseOrderStandard", CommonUtil.tryString(data.getFirst("PurchaseOrderStandard")));
		dicParam.addValue("vatExemptionYN", CommonUtil.tryString(data.getFirst("VatExemptionYN")));
		
		dicParam.addValue("routingId", CommonUtil.tryIntNull(data.getFirst("Routing_id")));
		dicParam.addValue("unitPrice", CommonUtil.tryFloatNull(data.getFirst("UnitPrice")));
		dicParam.addValue("user_id", CommonUtil.tryIntNull(data.getFirst("user_id").toString()));
		
		String sql = "";
		
		if(id == null) {
			sql = """
					INSERT INTO public.material
						("_created"
						,"_creater_id"
						, "Code" 
						, "Name" 
						, "MaterialGroup_id" 
						, "Unit_id" 
						, "Factory_id" 
						, "WorkCenter_id" 
						, "Equipment_id" 
						, "StoreHouse_id" 
						, "StoreHouseLoc" 
						, "ManagementLevel" 
						, "SafetyStock" 
						, "MaxStock" 
						, "ProcessSafetyStock" 
						, "ValidDays"
						, "LotUseYN" 
						, "PackingUnitQty" 
						, "PackingUnitName" 
						, "MinOrder" 
						, "MaxOrder" 
						, "LotSize" 
						, "LeadTime"
						, "StandardTime" 
						, "StandardTimeUnit" 
						, "Thickness" 
						, "Width" 
						, "Length" 
						, "Height" 
						, "Weight" 
						, "Color" 
						, "Usage" 
						, "Class1" 
						,  "Class2" 
						, "Class3" 
						, "Standard1" 
						, "Standard2" 
						, "Description" 
						, "CustomerBarcode" 
						, "InputManCount" 
						, "InputManHour" 
						, "PurchaseOrderStandard" 
						, "VatExemptionYN" 
						, "Routing_id" 
						, "UnitPrice"
						 , "Mtyn"
						 , "Useyn"
						 ,"Avrqty"
						 )
						VALUES
						(now()
						, :user_id
						, :code
						, :name
						, :matGroupId
						, :unitId
						, :factoryId
						, :workcenterId
						, :equipmentId
						, :storeHouseId
						, :storeHouseLoc
						, :managementLevel
						, :safetyStock
						, :maxStock
						, :processSafetyStock
						, :validDays
						, :lotUseYN
						, :packingUnitQty
						, :packingUnitName
						, :minOrder
						, :maxOrder
						, :lotSize
						, :leadTime
						, :standardTime
						, :standardTimeUnit
						, :thickness
						, :width
						, :length
						, :height
						, :weight
						, :color
						, :usage
						, :class1
						, :class2
						, :class3
						, :standard1
						, :standard2
						, :description
						, :customerBarcode
						, :inputManCount
						, :inputManHour
						, :purchaseOrderStandard
						, :vatExemptionYN
						, :routingId
						, :unitPrice
						, :mtyn
						, :useyn
						, :avrqty)
					""";
		}else {
			sql = """
					UPDATE public.material
					SET "_modified" = now()
					, "_modifier_id" = :user_id
					, "Code" = :code
					, "Name" = :name
					, "MaterialGroup_id" = :matGroupId
					, "Unit_id" = :unitId
					, "Factory_id" = :factoryId
					, "WorkCenter_id" = :workcenterId
					, "Equipment_id" = :equipmentId
					, "StoreHouse_id" = :storeHouseId
					, "StoreHouseLoc" = :storeHouseLoc
					, "ManagementLevel" = :managementLevel 
					, "SafetyStock" = :safetyStock
					, "MaxStock" = :maxStock
					, "ProcessSafetyStock" = :processSafetyStock
					, "ValidDays" = :validDays
					, "LotUseYN" = :lotUseYN
					, "PackingUnitQty" = :packingUnitQty 
					, "PackingUnitName" = :packingUnitName
					, "MinOrder" = :minOrder
					, "MaxOrder" = :maxOrder
					, "LotSize" = :lotSize
					, "LeadTime" = :leadTime
					, "StandardTime" = :standardTime
					, "StandardTimeUnit" = :standardTimeUnit
					, "Thickness" = :thickness
					, "Width" = :width
					, "Length" = :length
					, "Height" = :height
					, "Weight" = :weight
					, "Color" = :color
					, "Usage" = :usage
					, "Class1" = :class1
					, "Class2" = :class2
					, "Class3" = :class3
					, "Standard1" = :standard1
					, "Standard2" = :standard2
					, "Description" = :description 
					, "CustomerBarcode" = :customerBarcode
					, "InputManCount" = :inputManCount
					, "InputManHour" = :inputManHour
					, "PurchaseOrderStandard" = :purchaseOrderStandard
					, "VatExemptionYN" = :vatExemptionYN 
					, "Routing_id" = :routingId
					, "UnitPrice" = :unitPrice
					, "Mtyn" = :mtyn
					, "Useyn" = :useyn
					,"Avrqty" = :avrqty
					WHERE id = :id
					""";
		}
		
		
		
		return this.sqlRunner.execute(sql, dicParam);
	}
	
	public int deleteMaterial(int matPK){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("mat_pk", matPK);
        String sql = "";
        
        //품목에 연결된 단가삭제
        sql = " delete from mat_comp_uprice where \"Material_id\" = :mat_pk";
        this.sqlRunner.execute(sql, dicParam);
        
        //품목 삭제
    	sql = " delete from material where id = :mat_pk";
    	return this.sqlRunner.execute(sql, dicParam);
	}
}
