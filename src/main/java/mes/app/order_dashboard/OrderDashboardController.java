package mes.app.order_dashboard;

import mes.app.order_dashboard.service.OrderDashboardService;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orderDashboard")
public class OrderDashboardController {

    @Autowired
    private OrderDashboardService orderDashboardService;

    @GetMapping("/read")
    public AjaxResult getList(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                              @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                              @RequestParam(value = "search_type", required = false) String searchType,
             Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = orderDashboardService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd(user.getSpjangcd());
        tbDa006WPk.setCustcd((String) userInfo.get("custcd"));
        String saupnum = (String) userInfo.get("saupnum");
        String cltcd = (String) userInfo.get("cltcd");
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.orderDashboardService.getOrderList(tbDa006WPk,
                search_startDate, search_endDate, searchType, saupnum, cltcd);
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
    @GetMapping("/initDatas")
    public AjaxResult initDatas(Authentication auth){
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        Map<String, Object> userInfo = orderDashboardService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd(user.getSpjangcd());
        tbDa006WPk.setCustcd("JNJ");
        String cltcd = (String) userInfo.get("comp_id");
        List<Map<String, Object>> items = this.orderDashboardService.initDatas(tbDa006WPk, cltcd);
        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    @GetMapping("/readCalenderGrid")
    public AjaxResult getList2(Authentication auth) {
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();  // 유저 사업자번호(id)
        Map<String, Object> userInfo = orderDashboardService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        List<Map<String, Object>> items = this.orderDashboardService.getOrderList2(username);
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


}
