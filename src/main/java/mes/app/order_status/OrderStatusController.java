package mes.app.order_status;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mes.app.order_status.service.OrderStatusService;
import mes.domain.entity.User;
import mes.domain.entity.UserCode;
import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.model.AjaxResult;
import mes.domain.repository.UserCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/api/order_status")
public class OrderStatusController {

    @Autowired
    OrderStatusService orderStatusService;
    @Autowired
    private UserCodeRepository userCodeRepository;

    @GetMapping("/read")
    public AjaxResult orderStatusRead(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "search_spjangcd", required = false) String searchSpjangcd,
            @RequestParam(required = false) String searchCltnm,
            @RequestParam(required = false) String searchtketnm,
            @RequestParam(required = false) String searchstate,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        log.info("주문 확인 read 들어온 데이터:startDate{}, endDate{}, searchSpjangcd{}, searchCltnm{},searchstate{} ", startDate, endDate, searchSpjangcd, searchCltnm, searchstate);
        try {
            // 로그인한 사용자 정보에서 이름(perid) 가져오기
            User user = (User) auth.getPrincipal();
            String perid = user.getFirst_name(); // 이름을 가져옴
            String spjangcd = searchSpjangcd;

            // 서비스에서 데이터 가져오기
            List<Map<String, Object>> orderStatusList = orderStatusService.getOrderStatusByOperid(startDate, endDate, perid, spjangcd, searchCltnm, searchtketnm, searchstate);

            // ObjectMapper를 사용하여 hd_files 처리
            ObjectMapper objectMapper = new ObjectMapper();
            for (Map<String, Object> item : orderStatusList) {
                if (item.get("hd_files") != null) {
                    try {
                        // JSON 문자열을 List<Map<String, Object>>로 변환
                        List<Map<String, Object>> fileitems = objectMapper.readValue(
                                (String) item.get("hd_files"),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );

                        // fileitems를 순회하며 필요한 처리 수행
                        for (Map<String, Object> fileitem : fileitems) {
                            if (fileitem.get("filepath") != null && fileitem.get("fileornm") != null) {
                                String filenames = (String) fileitem.get("fileornm");
                                String filepaths = (String) fileitem.get("filepath");
                                String filesvnms = (String) fileitem.get("filesvnm");

                                List<String> fileornmList = filenames != null ? Arrays.asList(filenames.split(",")) : Collections.emptyList();
                                List<String> filepathList = filepaths != null ? Arrays.asList(filepaths.split(",")) : Collections.emptyList();
                                List<String> filesvnmList = filesvnms != null ? Arrays.asList(filesvnms.split(",")) : Collections.emptyList();

                                item.put("isdownload", !fileornmList.isEmpty() && !filepathList.isEmpty());
                            } else {
                                item.put("isdownload", false);
                            }
                        }

                        // 처리된 fileitems를 item에 업데이트
                        item.remove("hd_files");
                        item.put("hd_files", fileitems);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

    @GetMapping("/ModalRead")
    public AjaxResult ModalRead(@RequestParam(required = false) String searchTerm) {
        AjaxResult result = new AjaxResult();

        try {
            List<Map<String, Object>> modalList = orderStatusService.getModalListByClientName(searchTerm);

            result.success = true;
            result.data = modalList;
            result.message = "데이터 조회 성공";

        } catch (Exception e) {
            // 오류 발생 시 실패 상태 설정
            result.success = false;
            result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }

    @GetMapping("/searchData")
    public ResponseEntity<Map<String, Object>> searchData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchCltnm,
            @RequestParam(required = false) String searchtketnm,
            @RequestParam(required = false) String searchstate) {

        // 검색 결과를 서비스에서 가져오기
        List<Map<String, Object>> result = orderStatusService.searchData(startDate, endDate, searchCltnm, searchtketnm, searchstate);

        // 응답 데이터를 "data" 키로 래핑하여 JSON 형식으로 반환
        Map<String, Object> response = new HashMap<>();
        response.put("data", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getOrdtext")
    public AjaxResult getOrdtext(@RequestParam("reqdate") String reqdate, @RequestParam("remark") String remark) {
        System.out.println("요청사항 팝업 들어옴: reqdate = " + reqdate + ", remark = " + remark);

        AjaxResult result = new AjaxResult();
        try {
            String ordtextData = orderStatusService.getOrdtextByParams(reqdate, remark);

            result.success = true;
            result.data = ordtextData;
            result.message = "데이터 조회 성공";

        } catch (Exception e) {
            result.success = false;
            result.message = "데이터를 가져오는 중 오류가 발생했습니다.";
            e.printStackTrace(); // 오류 로그 출력
        }

        return result;
    }

    @GetMapping("/readCalenderGrid")
    public AjaxResult getList(@RequestParam(value = "search_spjangcd", required = false) String searchSpjangcd,
                              @RequestParam(value = "search_startDate", required = false) String searchStartDate,
                              @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                              @RequestParam(value = "search_type", required = false) String searchType,
                              Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();  // 유저 사업자번호(id)
        Map<String, Object> userInfo = orderStatusService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd(searchSpjangcd);
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.orderStatusService.getOrderList(tbDa006WPk,
                search_startDate, search_endDate, searchType);
        for (Map<String, Object> item : items) {
            if (item.get("ordflag").equals("0")) {
                item.remove("ordflag");
                item.put("ordflag", "주문접수");
            } else if (item.get("ordflag").equals("1")) {
                item.remove("ordflag");
                item.put("ordflag", "출하확인");
            } else if (item.get("ordflag").equals("2")) {
                item.remove("ordflag");
                item.put("ordflag", "주문확정");
            } else if (item.get("ordflag").equals("3")) {
                item.remove("ordflag");
                item.put("ordflag", "제작진행");
            } else if (item.get("ordflag").equals("4")) {
                item.remove("ordflag");
                item.put("ordflag", "출고");
            } else if (item.get("ordflag").equals("5")) {
                item.remove("ordflag");
                item.put("ordflag", "주문취소");
            }
            // 날짜 형식 변환 (reqdate)
            if (item.containsKey("reqdate")) {
                String setupdt = (String) item.get("reqdate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("reqdate", formattedDate);
                }
            }
            // 날짜 형식 변환 (deldate)
            if (item.containsKey("deldate")) {
                String setupdt = (String) item.get("deldate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("deldate", formattedDate);
                }
            }
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/isAdmin")
    public AjaxResult isAdmin(Authentication auth) {
        User user = (User) auth.getPrincipal();
        int userCode = user.getUserProfile().getUserGroup().getId();
        // 사용자의 권한이 일반거래처(code값 : 35)인지확인
        String userAuth;
        if(userCode == 35){
            userAuth = "nomal";
        }else {
            userAuth = "admin";
        }
        AjaxResult result = new AjaxResult();
        result.data = userAuth;
        return result;
    }

    @GetMapping("/initDatas")
    public AjaxResult initDatas(@RequestParam(value = "search_spjangcd", required = false) String searchSpjangcd,
                                @RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd(searchSpjangcd);
        String startDate = (searchStartDate).replaceAll("-","");
        String endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.orderStatusService.initDatas(tbDa006WPk, startDate, endDate);
        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }

    @GetMapping("/readCalenderGrid2")
    public AjaxResult getList2(@RequestParam(value = "search_spjangcd", required = false) String searchSpjangcd
                               , Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();  // 유저 사업자번호(id)
        Map<String, Object> userInfo = orderStatusService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd(searchSpjangcd);
        List<Map<String, Object>> items = this.orderStatusService.getOrderList2(tbDa006WPk);
        for (Map<String, Object> item : items) {
            if (item.get("ordflag").equals("0")) {
                item.remove("ordflag");
                item.put("ordflag", "주문접수");    //주문 접수
            } else if (item.get("ordflag").equals("1")) {
                item.remove("ordflag");
                item.put("ordflag", "출하완료");    //출하 완료
            } else if (item.get("ordflag").equals("2")) {
                item.remove("ordflag");
                item.put("ordflag", "주문확정");
            } else if (item.get("ordflag").equals("3")) {
                item.remove("ordflag");
                item.put("ordflag", "제작진행");
            } else if (item.get("ordflag").equals("4")) {
                item.remove("ordflag");
                item.put("ordflag", "출고");
            } else if (item.get("ordflag").equals("5")) {
                item.remove("ordflag");
                item.put("ordflag", "주문취소");
            }
            // 날짜 형식 변환 (reqdate)
            if (item.containsKey("reqdate")) {
                String setupdt = (String) item.get("reqdate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("reqdate", formattedDate);
                }
            }
            // 날짜 형식 변환 (deldate)
            if (item.containsKey("deldate")) {
                String setupdt = (String) item.get("deldate");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("deldate", formattedDate);
                }
            }
        }
        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> UpdateOrdflag(@RequestBody Map<String, Object> formData) {
        Map<String, Object> response = new HashMap<>();
        try {
            //log.info("출하 확인 데이터: {}", formData);

            Object ordersObj = formData.get("orders");

            if (!(ordersObj instanceof List)) {
                return ResponseEntity.badRequest().body(Map.of("message", "잘못된 데이터 형식입니다. 'orders'는 리스트여야 합니다."));
            }

            List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersObj;

            if (orders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "수정할 주문이 없습니다."));
            }

            // 한글을 숫자로 변환만 수행 (토글 변환 X)
            List<Map<String, Object>> validOrders = orders.stream()
                .map(order -> {
                    Object ordflagObj = order.get("ordflag");
                    if (ordflagObj != null && "0".equals(ordflagObj.toString())) {
                        order.put("ordflag", "1");   // ✅ 0 → 1 변환
                    }
                    return order;
                })
                .filter(order -> "1".equals(order.get("ordflag"))) // 변환된 것만 유지
                .collect(Collectors.toList());

            if (validOrders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "'주문 접수' 상태일 때만 수정이 가능합니다."));
            }

            // 서비스 호출 (서비스에서 상태 변환 수행)
            TB_DA006W updateResult = orderStatusService.UpdateOrdflag(validOrders);

            // 성공 응답 구성
            response.put("success", true);
            response.put("message", "주문 상태가 변경되었습니다.");
            response.put("data", updateResult);
            log.info("저장 완료: {}", updateResult);

        } catch (Exception e) {
            log.error("❌ 저장 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "저장 중 오류가 발생했습니다. 관리자에게 문의하세요.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/CancelOrder")
    public ResponseEntity<Map<String, Object>> CancelOrderUpdateOrdflag(@RequestBody Map<String, Object> formData) {
        Map<String, Object> response = new HashMap<>();
        try {
            //log.info("📌 주문 취소 요청 데이터: {}", formData);

            Object ordersObj = formData.get("orders");

            // 'orders' 값이 리스트인지 검증
            if (!(ordersObj instanceof List)) {
                return ResponseEntity.badRequest().body(Map.of("message", "잘못된 데이터 형식입니다. 'orders'는 리스트여야 합니다."));
            }

            List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersObj;

            // 주문이 비어있는 경우 처리
            if (orders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "수정할 주문이 없습니다."));
            }

            // "주문접수"(0) → "5"로 변환
            List<Map<String, Object>> validOrders = orders.stream()
                .map(order -> {
                    Object ordflagObj = order.get("ordflag");
                    if (ordflagObj != null) {
                        String ordflag = ordflagObj.toString();
                        // 0(주문등록) → 5(주문취소)
                        if ("0".equals(ordflag)) {
                            order.put("ordflag", "5");
                        }
                    }
                    return order;
                })
                .filter(order -> "5".equals(order.get("ordflag"))) // 변환된 것만 유지
                .collect(Collectors.toList());

            // 변환된 주문이 없는 경우 예외 처리
            if (validOrders.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "'주문 접수 상태만 수정이 가능합니다."));
            }

            // 서비스 호출 (상태 업데이트 실행)
            int updatedCount = orderStatusService.CancelOrderUpdateOrdflag(validOrders);

            // 성공 응답
            response.put("success", true);
            response.put("message", "✅ 주문 상태가 성공적으로 변경되었습니다.");
            response.put("updatedCount", updatedCount);
            log.info("✅ 주문 상태 변경 완료 ({}건)", updatedCount);

        } catch (Exception e) {
            log.error("❌ 저장 중 오류 발생", e);
            response.put("success", false);
            response.put("message", "저장 중 오류가 발생했습니다. 관리자에게 문의하세요.");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ordFlagType")
    public AjaxResult ordFlagType(
        @RequestParam(value = "parentCode", required = false) String parentCode) {
        AjaxResult result = new AjaxResult();

        try {
            // parentCode를 기준으로 하위 그룹 필터링
            List<UserCode> data = (parentCode != null)
                ? userCodeRepository.findByParentId(userCodeRepository.findByCode(parentCode).stream().findFirst().get().getId())
                : userCodeRepository.findAll();

            // 성공 시 데이터와 메시지 설정
            result.success = true;
            result.message = "데이터 조회 성공";
            result.data = data;

        } catch (Exception e) {
            // 예외 발생 시 처리
            result.success = false;
            result.message = "데이터 조회 중 오류 발생: " + e.getMessage();
        }

        return result;
    }

}
