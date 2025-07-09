package mes.app.request;

import mes.app.definition.service.BomService;
import mes.app.request.service.RequestService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/request/request2")
public class RequestController {

  @Autowired
  RequestService requestService;

  @Autowired
  BomService bomService;

  //bom 리스트 read
  @GetMapping("/bomList")
  public AjaxResult getBomList(@RequestParam(value = "Material_id")Integer Material_id){
    AjaxResult result = new AjaxResult();
    List<Map<String, Object>> bomList = requestService.getBomList(Material_id);
    if (bomList.isEmpty()) {
      result.success = false;
      result.message = "BOM 정보가 없습니다.";
      return result;
    }
    Integer bomId = (Integer) bomList.get(0).get("id");

    List<Map<String, Object>> bomTree = bomService.getBomComponentTreeList(bomId);
    result.data = bomTree;
    return result;
  }

}
