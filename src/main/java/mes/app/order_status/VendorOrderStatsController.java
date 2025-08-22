package mes.app.order_status;

import lombok.extern.slf4j.Slf4j;
import mes.app.order_status.service.VendorOrderStatsService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/VendorOrderStats")
public class VendorOrderStatsController { //업체별 주문 통계

  @Autowired
  VendorOrderStatsService vendorOrderStatsService;

  @GetMapping("/read")
  public AjaxResult orderStatusRead(
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate,
      @RequestParam(value = "search_spjangcd", required = false) String searchSpjangcd,
      @RequestParam(value = "searchCltnm", required = false) String searchCltnm) {
    AjaxResult result = new AjaxResult();
    log.info("업체별 주문 통계 read 들어온 데이터:startDate{}, endDate{}, searchSpjangcd{} , searchCltnm{} ", startDate, endDate, searchSpjangcd, searchCltnm);
    try {

      // 서비스에서 데이터 가져오기
      List<Map<String, Object>> orderStatusList = vendorOrderStatsService.getOrderStatusByOperid(startDate, endDate,searchSpjangcd,  searchCltnm);

      for (Map<String, Object> order : orderStatusList) {
        // 날짜 포맷 적용
        Object reqdateValue = order.get("reqdate");
        if (reqdateValue != null && reqdateValue instanceof String) {
          String reqdateStr = (String) reqdateValue;

          try {
            if (reqdateStr.length() == 8) { // "yyyyMMdd" 형식인지 확인
              String formattedDate = reqdateStr.substring(0, 4) + "-" + reqdateStr.substring(4, 6) + "-" + reqdateStr.substring(6, 8);
              order.put("reqdate", formattedDate);
            } else {
              order.put("reqdate", "잘못된 날짜 형식"); // 길이가 8이 아니면 오류 처리
            }
          } catch (Exception ex) {
            log.error("날짜 포맷 변환 중 오류 발생: {}", ex.getMessage());
            order.put("reqdate", "잘못된 날짜 형식");
          }
        }

        Object deldateValue = order.get("deldate");
        if (deldateValue != null && deldateValue instanceof String) {
          String deldateStr = (String) deldateValue;

          try {
            if (deldateStr.length() == 8) { // "yyyyMMdd" 형식인지 확인
              String formattedDate = deldateStr.substring(0, 4) + "-" + deldateStr.substring(4, 6) + "-" + deldateStr.substring(6, 8);
              order.put("deldate", formattedDate);
            } else {
              order.put("deldate", "잘못된 날짜 형식"); // 길이가 8이 아니면 오류 처리
            }
          } catch (Exception ex) {
            log.error("날짜 포맷 변환 중 오류 발생: {}", ex.getMessage());
            order.put("deldate", "잘못된 날짜 형식");
          }
        }

        // 전화번호 포맷 적용
        Object telnoValue = order.get("telno");
        if (telnoValue instanceof String) {
          String formattedTelno = formatPhoneNumber((String) telnoValue);
          order.put("telno", formattedTelno);
        }
      }

      // AjaxResult 설정
      result.success = true;
      result.data = orderStatusList;
      result.message = "데이터 조회 성공";

    } catch (Exception e) {
      // 오류 발생 시 실패 상태 설정
      result.success = false;
      result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
    }

    return result;
  }

  private String formatPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || !phoneNumber.matches("\\d+")) { // 숫자가 아니면 공백 반환
      return " ";
    }

    if (phoneNumber.length() == 11) { // 11자리 (예: 01012345678 → 010-1234-5678)
      return phoneNumber.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
    } else if (phoneNumber.length() == 12) { // 12자리 (예: 03112345678 → 031-1234-5678)
      return phoneNumber.replaceAll("(\\d{3})(\\d{4})(\\d{5})", "$1-$2-$3");
    }

    return ""; // 그 외의 경우 빈 문자열 반환
  }

  //차트 데이터
  @GetMapping("/getChartData")
  public AjaxResult getChartData(
      Authentication auth,
      @RequestParam(value = "search_spjangcd", required = false) String spjangcd,
      @RequestParam(value = "startDate", required = false) String startDate,
      @RequestParam(value = "endDate", required = false) String endDate,
      @RequestParam(value = "searchCltnm", required = false) String searchCltnm) {

    AjaxResult result = new AjaxResult();
    try {

      log.info("업체별주문통계 차트 search_spjangcd:{}, startDate: {}, endDate: {}, cltnm: {}", spjangcd, startDate, endDate, searchCltnm);

      List<Map<String, Object>> rawData = vendorOrderStatsService.getChartData(spjangcd, startDate, endDate, searchCltnm);

      if (rawData == null || rawData.isEmpty()) {
        log.warn("조회된 데이터가 없습니다.");
        result.success = false;
        result.message = "조회된 데이터가 없습니다.";
      } else {
        //log.info("쿼리 결과 데이터: {}", rawData);
        result.success = true;
        result.data = rawData;
      }
    } catch (Exception e) {
      log.error("데이터 처리 중 오류: {}", e.getMessage(), e);
      result.success = false;
      result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
    }

    return result;
  }

  @GetMapping("/SalesRead")
  public AjaxResult MonthlySalesList(@RequestParam(value="cboYear",required=false) String cboYear,
                                     @RequestParam(value="cboCompany",required=false) Integer cboCompany,
                                     @RequestParam(value = "spjangcd") String spjangcd){

    List<Map<String,Object>> items = this.vendorOrderStatsService.getSalesList(cboYear,cboCompany, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/SalesChartRead")
  public AjaxResult getSalesChartRead(@RequestParam(value="cboYear",required=false) String cboYear,
                                      @RequestParam(value="cboCompany",required=false) Integer cboCompany,
                                      @RequestParam(value = "spjangcd") String spjangcd){
    AjaxResult result = new AjaxResult();

    try {

      List<Map<String, Object>> SalesChart = vendorOrderStatsService.getSalesChartRead(cboYear,cboCompany, spjangcd);

      if (SalesChart == null || SalesChart.isEmpty()) {
        log.warn("조회된 데이터가 없습니다.");
        result.success = false;
        result.message = "조회된 데이터가 없습니다.";
      } else {
        //log.info("쿼리 결과 데이터: {}", rawData);
        result.success = true;
        result.data = SalesChart;
      }

    } catch (Exception e) {
      log.error("데이터 처리 중 오류: {}", e.getMessage(), e);
      result.success = false;
      result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
    }

    return result;
  }

  @GetMapping("/DepositRead")
  public AjaxResult getMonthDepositList(@RequestParam(value="cboYear",required=false) String cboYear,
                                     @RequestParam(value="cboCompany",required=false) Integer cboCompany,
                                     @RequestParam(value = "spjangcd") String spjangcd){

    List<Map<String,Object>> items = this.vendorOrderStatsService.getPurchaseList(cboYear,cboCompany, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/DepositChartRead")
  public AjaxResult getDepositChartRead(@RequestParam(value="cboYear",required=false) String cboYear,
                                      @RequestParam(value="cboCompany",required=false) Integer cboCompany,
                                      @RequestParam(value = "spjangcd") String spjangcd){
    AjaxResult result = new AjaxResult();

    try {

      List<Map<String, Object>> DepositChart = vendorOrderStatsService.getDepositChartRead(cboYear,cboCompany, spjangcd);

      if (DepositChart == null || DepositChart.isEmpty()) {
        log.warn("조회된 데이터가 없습니다.");
        result.success = false;
        result.message = "조회된 데이터가 없습니다.";
      } else {
        //log.info("쿼리 결과 데이터: {}", rawData);
        result.success = true;
        result.data = DepositChart;
      }

    } catch (Exception e) {
      log.error("데이터 처리 중 오류: {}", e.getMessage(), e);
      result.success = false;
      result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
    }

    return result;
  }

}
