package mes.app.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.inventory.service.LotService;
import mes.app.inventory.service.MaterialInoutService;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.*;
import mes.domain.services.CommonUtil;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/material_inout")
public class MaterialInoutController {
	
	@Autowired
	private MaterialInoutService materialInoutService;
	
	@Autowired
	private LotService lotService;
	
	@Autowired
	MatInoutRepository matInoutRepository;
	
	@Autowired
	MaterialRepository materialRepository;
	
	@Autowired
	MatLotRepository matLotRepository;
	
	@Autowired
	TestResultRepository testResultRepository;
	
	@Autowired
	TestItemResultRepository testItemResultRepository;

	@Autowired
	BujuRepository bujuRepository;
	
	@GetMapping("/read")
	public AjaxResult getMaterialInout(
			@RequestParam(value = "srchStartDt", required=false) String srchStartDt,
			@RequestParam(value = "srchEndDt", required=false) String srchEndDt,
			@RequestParam(value = "house_pk", required=false) String housePk,
			@RequestParam(value = "mat_type", required=false) String matType,
			@RequestParam(value = "mat_grp_pk", required=false) String matGrpPk,
			@RequestParam(value = "keyword", required=false) String keyword) {
		
        List<Map<String, Object>> items = this.materialInoutService.getMaterialInout(srchStartDt,srchEndDt,housePk,matType,matGrpPk,keyword);      
   		
        AjaxResult result = new AjaxResult();
        result.data = items;        				
        
		return result;
	}
	
	@PostMapping("/save")
	@Transactional
	public AjaxResult saveMaterialInout(
			@RequestParam("Description") String description,
			@RequestParam("InoutQty") String inoutQty,
			@RequestParam("InoutType") String inoutType,
			@RequestParam("Material_id") String materialId,
			@RequestParam("StoreHouse_id") String storeHouseId,
			@RequestParam("cboMaterialGroup") String cboMaterialGroup,
			@RequestParam("cboMaterialType") String cboMaterialType,
			@RequestParam("type") String type,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		// 현재 일자
		LocalDate date = LocalDate.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		// 현재 시간
		LocalTime time = LocalTime.now();
		DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
		
		Integer matPk = Integer.parseInt(materialId);
		String state = "confirmed";
		String _status = "a";
		Integer qty = Integer.parseInt(inoutQty);
		
		result.success = false;
		
		MaterialInout mi = new MaterialInout();
		mi.setInoutDate(LocalDate.parse(date.format(dateFormat)));
		mi.setInoutTime(LocalTime.parse(time.format(timeFormat)));
		mi.setMaterialId(matPk);
		mi.setStoreHouseId(Integer.parseInt(storeHouseId));
		
		Material m = this.materialRepository.getMaterialById(matPk);
		
		String testYn = m.getInTestYN() != null ? m.getInTestYN() : "";
		
		if (type.equals("in")) {
			mi.setInOut("in");
			mi.setInputType(inoutType);
			if(testYn.equals("Y")) {
				mi.setPotentialInputQty((float)qty);
				state = "waiting";
				_status = "t";
			} else {
				mi.setInputQty((float)qty);
			}
		} else  {
			mi.setInOut("out");
			mi.setOutputType(inoutType);
			mi.setOutputQty((float)qty);
		}
		mi.setDescription(description);
		mi.setState(state);
		mi.set_status(_status);
		mi.set_audit(user);
		this.matInoutRepository.save(mi);
		
		result.success = true;
		
		return result;
	}
	
	// 엑셀데이터 그리드로 변환
	@SuppressWarnings("unchecked")
	@GetMapping("/trans_multi_input_data")
	public AjaxResult transMultiInputData(
			@RequestParam MultiValueMap<String,Object> Q
			) throws JSONException, JsonMappingException, JsonProcessingException {
		
		AjaxResult result = new AjaxResult();
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
		
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
				
		for(int i = 0; i < data.size(); i++) {
			if(data.get(i).get("mat_code").toString().isEmpty()) {
				continue;
			}
			JSONObject row = new JSONObject();
			Material m = this.materialRepository.findByCode(data.get(i).get("mat_code").toString());
			if (m != null) {
				row.put("mat_name", m.getName());
			}
			row.put("input_qty", data.get(i).get("input_qty").toString());
			row.put("mat_code", data.get(i).get("mat_code").toString());
			Map<String, Object> map = new ObjectMapper().readValue(row.toString(), Map.class) ;
			items.add(map);
			
		}
		result.data = items;
		return result;
	}
	
	@PostMapping("/save_multi_data")
	@Transactional
	public AjaxResult saveMultiData(
			@RequestParam("Company_id") String companyId,
			@RequestParam("InoutType") String inoutType,
			@RequestParam MultiValueMap<String,Object> Q,
			@RequestParam("StoreHouse_id") String storeHouseId,
			@RequestParam("type") String type,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		// 현재 일자
		LocalDate date = LocalDate.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		// 현재 시간
		LocalTime time = LocalTime.now();
		DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
		
		String state = "confirmed";
		String _status = "a";
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
		
		AjaxResult result = new AjaxResult();
		
		result.success = false;
		for (int i=0; i < data.size(); i++) {
			if(data.get(i).get("mat_code").toString().isEmpty()) {
				continue;
			}
			
			Material m = this.materialRepository.findByCode(data.get(i).get("mat_code").toString());
			String testYn = m.getInTestYN() != null ? m.getInTestYN() : "";
			Integer matId = m.getId();
			Integer qty = Integer.parseInt(data.get(i).get("input_qty").toString());
			
			MaterialInout mi = new MaterialInout();
			mi.setMaterialId(matId);
			mi.setInoutDate(LocalDate.parse(date.format(dateFormat)));
			mi.setInoutTime(LocalTime.parse(time.format(timeFormat)));
			mi.setCompanyId(CommonUtil.tryIntNull(companyId));
			mi.setStoreHouseId(Integer.parseInt(storeHouseId));
			
			if (type.equals("in")) {
				mi.setInOut("in");
				mi.setInputType(inoutType);
				if(testYn.equals("Y")) {
					mi.setPotentialInputQty((float)qty);
					state = "waiting";
					_status = "t";
				} else {
					mi.setInputQty((float)qty);
					state = "confirmed";
					_status = "a";
				}
			} else {
				mi.setInOut("out");
				mi.setOutputType(inoutType);
				mi.setOutputQty((float)qty);
			}
			mi.setState(state);
			mi.set_status(_status);
			mi.set_audit(user);
			mi = this.matInoutRepository.save(mi);
			
			
		}
		result.success = true;
		
		return result;
	}
	
	@GetMapping("/mio_lot_list")
	public AjaxResult mioLotList(
			@RequestParam("mio_id") String mioId) {
		
		List<Map<String, Object>> items = this.lotService.mioLotList(mioId);
		AjaxResult result = new AjaxResult();
		result.data = items;
		return result;
	}
	
	@GetMapping("/mio_test_list")
	public AjaxResult mioTestList(
			@RequestParam("mio_id") Integer mioId) {
		
		
		List<TestResult> trList = this.testResultRepository.findBySourceTableNameAndSourceDataPk("mat_inout", mioId);
		
		List<Map<String, Object>> items = null;
		Integer testMasterId = null;
		
		if (!trList.isEmpty()) {
			items = this.materialInoutService.mioTestList(mioId,trList.get(0).getId());
		} else {
			testMasterId = this.materialInoutService.getTestMasterByItem(mioId);

			if (testMasterId != null) {
				items = this.materialInoutService.prodTestListByTestMaster(testMasterId);
			} else{
				items = this.materialInoutService.mioTestDefaultList();
			}

		}
		
		Map<String, Object> effectDt = this.materialInoutService.getEffectDate(mioId);
		
		String effDt = effectDt.get("EffectiveDate") != null ? effectDt.get("EffectiveDate").toString() : null;
		
		
		Map<String, Object> item = new HashMap<>();
		
		item.put("EffectiveDate", effDt);
		item.put("testDate", items.get(0).get("testDate"));
		item.put("CheckName", items.get(0).get("CheckName"));
		item.put("JudgeCode", items.get(0).get("JudgeCode"));
		item.put("CharResult", items.get(0).get("CharResult"));
		item.put("testMasterId", items.get(0).get("testMasterId"));
		item.put("testResultId", items.get(0).get("testResultId"));
		item.put("mioList", items);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		return result;
	}
	
	@PostMapping("/lot_save")
	@Transactional
	public AjaxResult lotSave(
			@RequestBody MultiValueMap<String,Object> Q,
			@RequestParam("Material_id") String materialId,
			@RequestParam("StoreHouse_id") Integer storeHouseId,
			@RequestParam("mio_id") String mioId,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		Timestamp today = new Timestamp(System.currentTimeMillis());
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
		
		result.success = false;
		for (int i=0; i < data.size(); i++) {
			MaterialLot ml = null;
			String LotNumber = null;
			if (!data.get(i).get("LotNumber").toString().isEmpty()) {
				LotNumber = data.get(i).get("LotNumber").toString();
				ml = this.matLotRepository.getByLotNumber(LotNumber);
				if (data.get(i).get("Description") != null) {
					ml.setDescription(data.get(i).get("Description").toString());
				}
				this.matLotRepository.save(ml);
			} else {
				LotNumber = this.lotService.make_lot_in_number();
				String effectiveDate = data.get(i).get("EffectiveDate").toString() + " 00:00:00";
				ml = new MaterialLot();
				ml.setLotNumber(LotNumber);
				ml.setMaterialId(Integer.parseInt(materialId));
				ml.setInputQty(Float.parseFloat(data.get(i).get("InputQty").toString()));
				ml.setCurrentStock(Float.parseFloat(data.get(i).get("InputQty").toString()));
				ml.setInputDateTime(today);
				ml.setEffectiveDate(Timestamp.valueOf(effectiveDate));
				ml.setSourceTableName("mat_inout");
				ml.setSourceDataPk(Integer.parseInt(mioId));
				if (data.get(i).get("Description") != null) {
					ml.setDescription(data.get(i).get("Description").toString());
				}
				ml.setStoreHouseId(storeHouseId);
				ml.set_audit(user);
				ml = this.matLotRepository.save(ml);
			}
			
			result.success = true;
		}
		
		return result;
	}
	
	@PostMapping("/test_save")
	@Transactional
	public AjaxResult testSave(
			@RequestBody MultiValueMap<String,Object> Q,
			@RequestParam(value = "material_id", required = false) Integer materialId,
			@RequestParam(value = "testRemark", required = false) String testRemark,
			@RequestParam(value = "test_mast_id", required = false) String testMastId,
			@RequestParam(value = "test_result_id", required = false) String testResultId,
			@RequestParam(value = "judg_grp", required = false) String judgGrp,
			@RequestParam(value = "test_date", required = false) String test_date,
			@RequestParam(value = "effective_date", required = false) String effectiveDate,
			@RequestParam(value = "mio_id", required = false) Integer mioId,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		Timestamp testDate = Timestamp.valueOf(test_date+ " 00:00:00");
		
		if (StringUtils.hasText(testResultId)) {
			List<TestItemResult> trList = this.testItemResultRepository.findByTestResultId(Integer.parseInt(testResultId));
			
			// 결과 삭제
			if(trList.size() > 0) {
				for (int i = 0; i < trList.size(); i++) {
					this.testItemResultRepository.deleteById(trList.get(i).getId());
				}
			}
			
			this.testItemResultRepository.flush();
		}
		
		
		TestResult tr = new TestResult();
		
		if (StringUtils.hasText(testResultId)) {
			tr = this.testResultRepository.getTestResultById(Integer.parseInt(testResultId));
		} else {
			tr.setSourceDataPk(mioId);
			tr.setSourceTableName("mat_inout");
			tr.setMaterialId(materialId);
		}
		
		tr.setTestMasterId(Integer.parseInt(testMastId));
		tr.setTestDateTime(testDate);
		tr.set_audit(user);
		
		this.testResultRepository.saveAndFlush(tr);
		
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
		
		for(int i = 0; i < data.size(); i++) {
			TestItemResult tir = new TestItemResult();
			tir.setJudgeCode(judgGrp);
			tir.setTestDateTime(testDate);
			tir.setSampleID(String.valueOf(materialId) + "/" +mioId);
			tir.setCharResult(testRemark);
			tir.setTestItemId(Integer.parseInt(data.get(i).get("id").toString()));
			tir.setTestResultId(tr.getId());
			
			if(data.get(i).get("result1") != null) {
				tir.setChar1(data.get(i).get("result1").toString());
			}
			tir.set_audit(user);
			
			this.testItemResultRepository.save(tir);
		}
		
		MaterialInout mi = this.matInoutRepository.getMatInoutById(mioId);
		// 유효기간 변경
		if(StringUtils.hasText(effectiveDate)) {
			Timestamp effDt = Timestamp.valueOf(effectiveDate+ " 00:00:00");
			mi.setEffectiveDate(effDt);
		}

		mi.setState("confirmed");
		if (!"부적합".equals(judgGrp)) {
			// 적합한 경우에만 입고 처리
			mi.setInputQty(mi.getPotentialInputQty());
			mi.setPotentialInputQty((float)0);

			// 트리거 작동용 상태 변경
			mi.set_status("a");
		}

		this.matInoutRepository.save(mi);
		
		Map<String, Object> item = new HashMap<>();
		item.put("id", mioId);
		
		result.data = item;
		
		return result;
	}
	
	@PostMapping("/check_in_test")
	@Transactional
	public AjaxResult checkInTest(
			@RequestBody MultiValueMap<String,Object> Q,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		List<Map<String, Object>> data = CommonUtil.loadJsonListMap(Q.getFirst("Q").toString());
	
		for(int i = 0; i < data.size(); i++) {
			Integer id = Integer.parseInt(data.get(i).get("id").toString());
			Float inputQty = Float.parseFloat(data.get(i).get("PotentialInputQty").toString());
			MaterialInout mi = this.matInoutRepository.getMatInoutById(id);
			mi.setInputQty(inputQty);
			mi.setPotentialInputQty((float)0);
			mi.setState("confirmed");
			mi.set_status("a");
			mi.set_audit(user);
			this.matInoutRepository.save(mi);
		}
		
		return result;
	}

	@GetMapping("/read_balju")
	public AjaxResult getbaljuList(
			@RequestParam(value="start", required=false) String start_date,
			@RequestParam(value="end", required=false) String end_date,
			HttpServletRequest request) {

		start_date = start_date + " 00:00:00";
		end_date = end_date + " 23:59:59";

		Timestamp start = Timestamp.valueOf(start_date);
		Timestamp end = Timestamp.valueOf(end_date);

		List<Map<String, Object>> items = this.materialInoutService.getBaljuList(start, end);

		AjaxResult result = new AjaxResult();
		result.data = items;

		return result;
	}

	@PostMapping("/save_balju")
	@Transactional
	public AjaxResult saveBaljuInout(
			@RequestBody List<Map<String, Object>> baljuList,
			HttpServletRequest request,
			Authentication auth) {

		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();

		for (Map<String, Object> item : baljuList) {
			try {
				Integer bal_pk = (Integer) item.get("id");
				String description = (String) item.get("Description2");
				if (description == null || description.trim().isEmpty()) {
					description = "발주 입고";
				}
				String inoutQtyStr = String.valueOf(item.get("inputQty")); // '입고 수량'
				String materialIdStr = String.valueOf(item.get("Material_id"));
				String storeHouseIdStr = String.valueOf(item.get("StoreHouse_id"));

				Integer matPk = Integer.parseInt(materialIdStr);
				Integer qty = Integer.parseInt(inoutQtyStr);

				MaterialInout mi = new MaterialInout();
				mi.setInoutDate(LocalDate.now());
				mi.setInoutTime(LocalTime.now());
				mi.setMaterialId(matPk);
				mi.setStoreHouseId(Integer.parseInt(storeHouseIdStr));

				Material m = materialRepository.getMaterialById(matPk);
				String testYn = m.getInTestYN() != null ? m.getInTestYN() : "";

				if ("Y".equals(testYn)) {
					mi.setPotentialInputQty((float) qty);
					mi.setState("waiting");
					mi.set_status("t");
				} else {
					mi.setInputQty((float) qty);
					mi.setState("confirmed");
					mi.set_status("a");
				}

				mi.setDescription(description);
				mi.setInOut("in");
				mi.set_audit(user);
				mi.setSourceDataPk(bal_pk);
				mi.setSourceTableName("balju");

				Balju balju = this.bujuRepository.getBujuById(bal_pk);
				double prevSujuQty2 = balju.getSujuQty2() != null ? balju.getSujuQty2() : 0.0;
				balju.setSujuQty2(prevSujuQty2 + qty);
				balju.setShipmentState(storeHouseIdStr);

				if (balju.getSujuQty() > balju.getSujuQty2()) {
					balju.setState("partial");
					mi.setInputType("발주 부분 입고");
				} else {
					balju.setState("received");
					mi.setInputType("발주 입고");
				}
				matInoutRepository.save(mi);
				bujuRepository.save(balju);

			} catch (Exception e) {
				result.success = false;
				result.message = "처리 중 오류 발생: " + e.getMessage();
				return result;
			}
		}
		result.success = true;

		return result;
	}
	
}
