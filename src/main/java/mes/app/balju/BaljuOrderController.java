package mes.app.balju;

import lombok.extern.slf4j.Slf4j;
import mes.app.balju.service.BaljuOrderService;
import mes.domain.entity.Balju;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.BujuRepository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/balju/balju_order")
public class BaljuOrderController {

  @Autowired
  BaljuOrderService baljuOrderService;

  @Autowired
  BujuRepository bujuRepository;

  // 발주 목록 조회
  @GetMapping("/read")
  public AjaxResult getSujuList(
      @RequestParam(value="date_kind", required=false) String date_kind,
      @RequestParam(value="start", required=false) String start_date,
      @RequestParam(value="end", required=false) String end_date,
      @RequestParam(value ="spjangcd") String spjangcd,
      HttpServletRequest request) {
    //log.info("발주 read--- date_kind:{}, start_date:{},end_date:{} , spjangcd:{} "
    // ,date_kind,start_date , end_date, spjangcd);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  // 발주 등록
  @PostMapping("/manual_save")
  public AjaxResult BaljuSave(
      @RequestParam(value="id", required=false) Integer id,
      @RequestParam(value="SujuQty") Integer sujuQty, //발주량
      @RequestParam(value="Company_id") Integer companyId,
      @RequestParam(value="CompanyName") String companyName,
      @RequestParam(value="Description", required=false) String description,
      @RequestParam(value="DueDate") String dueDate,
      @RequestParam(value="JumunDate") String jumunDate,
      @RequestParam(value="Material_id") Integer materialId,
      @RequestParam(value="AvailableStock", required=false) Float availableStock,
      @RequestParam(value="SujuType") String sujuType,
      @RequestParam(value = "BaljuUnitPrice")Double BaljuUnitPrice, //단가
      @RequestParam(value = "BaljuPrice")Double BaljuPrice,   //공급가
      @RequestParam(value = "BaljuVat")Double BaljuVat,     //부과세
      @RequestParam(value = "invatyn") String isVat,
      @RequestParam(value = "BaljuTotalPrice") Double totalAmount,
      @RequestParam(value = "spjangcd") String spjangcd,
      HttpServletRequest request,
      Authentication auth	) {
    /*log.info("발주 등록 요청: id={}, sujuQty={}, companyId={}, companyName={}, description={}, dueDate={}, jumunDate={}, materialId={}, 3" +
            "availableStock={}, sujuType={}, BaljuUnitPrice={} , BaljuPrice={}, BaljuVat={} , isVat={}, spjangcd = {}",
        id, sujuQty, companyId, companyName, description, dueDate, jumunDate, materialId, availableStock, sujuType, BaljuUnitPrice, BaljuPrice, BaljuVat,isVat,spjangcd );*/
    User user = (User)auth.getPrincipal();

    Balju balju = null;

    if (id != null) {
      balju = this.bujuRepository.getBujuById(id);
//      log.info("🔄 기존 발주 수정: id={}", id);
    } else {
      balju = new Balju();
      balju.setState("draft");
//      log.info("🆕 신규 발주 생성");
    }

    availableStock = availableStock==null?0:availableStock;
    Date due_Date = CommonUtil.trySqlDate(dueDate);
    Date jumun_Date = CommonUtil.trySqlDate(jumunDate);
    String jumunNumber = baljuOrderService.makeJumunNumber(jumun_Date);
    String isVatYn = "Y".equalsIgnoreCase(isVat) || "true".equalsIgnoreCase(isVat) ? "Y" : "N";

    balju.setSujuQty(Double.valueOf(sujuQty));
    balju.setSujuQty2(Double.valueOf(0));
    balju.setCompanyId(companyId);
    balju.setCompanyName(companyName);
    balju.setDescription(description);
    balju.setDueDate(due_Date);
    balju.setJumunDate(jumun_Date);
    balju.setMaterialId(materialId);
    balju.setAvailableStock(availableStock); // 없으면 0으로 보내기 추가
    balju.setSujuType(sujuType);
    balju.set_status("manual");
    balju.setJumunNumber(jumunNumber);
    balju.setUnitPrice(BaljuUnitPrice);
    balju.setPrice(BaljuPrice);
    balju.setVat(BaljuVat);
    balju.set_audit(user);
    balju.setInVatYN(isVatYn);
    balju.setSpjangcd(spjangcd);
    balju.setTotalAmount(totalAmount);

    balju = this.bujuRepository.save(balju);
//    log.info("✅ 발주 저장 완료: balju={}", balju);

    AjaxResult result = new AjaxResult();
    result.data=balju;
    return result;
  }

  // 발주 상세정보 조회
  @GetMapping("/detail")
  public AjaxResult getBaljuDetail(
      @RequestParam("id") int id,
      HttpServletRequest request) {
    Map<String, Object> item = this.baljuOrderService.getBaljuDetail(id);

    AjaxResult result = new AjaxResult();
    result.data = item;

    return result;
  }

  // 발주 삭제
  @PostMapping("/delete")
  public AjaxResult deleteSuju(
      @RequestParam("id") Integer id,
      @RequestParam("State") String State) {

    AjaxResult result = new AjaxResult();

    if (State.equals("draft")==false) {
      // draft 아닌것만
      result.success = false;
      result.message = "미입고 상태일 때만 삭제할 수 있습니다.";
      return result;
    }

    this.bujuRepository.deleteById(id);

    return result;
  }

  //중지 처리
  @PostMapping("/balju_stop")
  public AjaxResult balju_stop(@RequestParam(value="id", required=false) Integer id){

    List<Map<String, Object>> items = this.baljuOrderService.balju_stop(id);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  //단가 찾기
  @GetMapping("/price")
  public AjaxResult BaljuPrice(@RequestParam("mat_pk") int materialId,
                               @RequestParam("JumunDate") String jumunDate,
                               @RequestParam("company_id") int companyId){
    //log.info("발주단가 찾기 --- matPk:{}, ApplyStartDate:{},company_id:{} ",materialId,jumunDate , companyId);
    List<Map<String, Object>> items = this.baljuOrderService.getBaljuPrice(materialId, jumunDate, companyId);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

}
