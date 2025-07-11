package mes.app.request;

import mes.app.definition.service.BomService;
import mes.app.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.criteria.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/request/request")
public class RequestController {

  @Autowired
  RequestService requestService;

  @Autowired
  BomService bomService;

  @Autowired
  private TB_DA007WRepository tbDa007WRepository;

  @Autowired
  private TB_DA006WRepository tbDa006WRepository;

  @Autowired
  private TB_DA006WFILERepository tbDa006WFileRepository;

  @Autowired
  Settings settings;

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

  @PostMapping("/saveWithFiles")
  @Transactional
  public ResponseEntity<?> saveOrderWithFiles(
      @RequestPart("jsonData") Map<String, Object> data,
      @RequestPart("files") List<MultipartFile> files,
      Authentication auth
  ) {
    User user = (User) auth.getPrincipal();

    String custcd = (String) data.get("cboCompanyHidden");
    String spjangcd = (String) data.get("spjangcd");
    String reqdate = (String) data.get("reqdate");
    String reqnum = (String) data.get("reqnum");

    boolean isNew = (reqnum == null || reqnum.isBlank());

    if (isNew) {
      // 🔹 신규일 경우 reqnum 채번 (예: 1001부터)
      reqnum = tbDa006WRepository.getNextReqnum(custcd, spjangcd, reqdate);
    }

    // ✅ 1. TB_DA006W 저장 (헤더)
    TB_DA006W_PK pk = new TB_DA006W_PK(custcd, spjangcd, reqdate, reqnum);
    TB_DA006W head;
    if (isNew) {
      head = new TB_DA006W();
    } else {
      head = tbDa006WRepository.findById(String.valueOf(pk))
          .orElseThrow(() -> new RuntimeException("헤더 정보가 존재하지 않습니다."));
    }

    head.setId(pk);
    head.setCltcd(String.valueOf(data.get("cboCompanyHidden")));
    head.setCltnm((String) data.get("CompanyName"));  //회사이름
    head.setDeldate(String.valueOf(data.get("deldate"))); //납기 희망일
    head.setCltzipcd(String.valueOf(data.get("cltzipcd"))); //우편번호
    head.setCltaddr(String.valueOf(data.get("address1")));  //업체 주소
    head.setModeltxt((String) data.get("modeltxt"));  //모델명
    head.setSetsamt(Long.parseLong((String) data.get("setsamt")));  //공급기준
    head.setSetqty(Long.parseLong((String) data.get("setqty")));    //수량
    head.setAmount(Long.parseLong((String) data.get("amount")));    //공급계
    head.setOutamt(Long.parseLong((String) data.get("outamt")));    //외주계
    head.setEyunamt(Long.parseLong((String) data.get("eyunamt")));  //이윤
    head.setPereyunamt(Long.parseLong((String) data.get("pereyunamt")));  //개당이윤율
    head.setEyunyul(Double.parseDouble((String) data.get("eyunyul")));  //이윤율
    head.setToteyunamt(Long.parseLong((String) data.get("toteyunamt"))); //전체이윤
    head.setProjectno((String) data.get("projectno"));
    head.setInperid(user.getUsername());
    head.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    head.setTelno((String) data.get("telno"));
    tbDa006WRepository.save(head);

    // ✅ 2. TB_DA007W 저장 (상세항목들)
    tbDa007WRepository.deleteByPk(custcd, spjangcd, reqdate, reqnum);

    List<Map<String, Object>> detailList = (List<Map<String, Object>>) data.get("detailList");
    int seq = 1;
    for (Map<String, Object> row : detailList) {
      TB_DA007W_PK detailPk = new TB_DA007W_PK(custcd, spjangcd, reqdate, reqnum, String.format("%03d", seq++));
      TB_DA007W detail = new TB_DA007W();
      detail.setId(detailPk);
      detail.setModelnm((String) row.get("txtModelNm"));
      detail.setPname((String) row.get("pname"));
      detail.setQty(Double.parseDouble((String) row.get("qty")));
      detail.setSetamt(Long.parseLong((String) row.get("setamt")));
      detail.setSaleamt(Long.parseLong((String) row.get("saleamt")));
      detail.setUamt(Double.parseDouble((String) row.get("uamt")));
      detail.setRemark((String) row.get("remark"));
      detail.setJobflag((String) row.get("jobflag"));
      detail.setInperid(user.getUsername());
      detail.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
      tbDa007WRepository.save(detail);
    }

    /*// ✅ 3. TB_DA006W_FILE 저장
    for (MultipartFile file : files) {
      String uuid = UUID.randomUUID().toString();
      String savedName = uuid + "_" + file.getOriginalFilename();
      Path filePath = Paths.get(uploadDir, savedName);
      Files.copy(file.getInputStream(), filePath);

      TB_DA006WFile fileEntity = new TB_DA006WFile();
      fileEntity.setCustcd(custcd);
      fileEntity.setSpjangcd(spjangcd);
      fileEntity.setReqdate(reqdate);
      fileEntity.setReqnum(reqnum);
      fileEntity.setFilename(file.getOriginalFilename());
      fileEntity.setSavepath(savedName);
      fileEntity.setInperid(user.getUsername());
      fileEntity.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
      tbDa006WFileRepository.save(fileEntity);
    }*/

    return ResponseEntity.ok(Map.of("success", true, "message", "주문 저장 완료", "reqnum", reqnum));
  }


  /*@PostMapping("/saveWithFiles")
  @Transactional
  public ResponseEntity<?> saveOrderWithFiles(
      @RequestPart("jsonData") Map<String, Object> data,  //
      @RequestPart("files") List<MultipartFile> files,    // 파일
      Authentication auth
  ) {
    User user = (User) auth.getPrincipal();
    String spjangcd = (String) data.get("spjangcd");
    String reqdate = (String) data.get("reqdate");  //주문일자
    String deldate = (String) data.get("deldate");  //납품희말일
    String modeltxt = (String) data.get("modeltxt"); //model
    Integer cboCompanyHidden = (Integer) data.get("cboCompanyHidden");  //거래처코드
    String CompanyName = (String) data.get("CompanyName");  //거래처 명
    String projectno = (String) data.get("projectno"); // 프로젝트명
    String perid = (String) data.get("perid");  //담당자
    String postno = (String) data.get("postno");  //우편번호
    String address1 = (String) data.get("address1");  //상세주소
    String setsamt = (String) data.get("setsamt");  //공급기준
    String setqty = (String) data.get("setqty");  //수량
    String amount = (String) data.get("setsamt"); //공급계
    String outamt = (String) data.get("outamt"); //외주계
    String eyunamt = (String) data.get("eyunamt");  // 이윤
    String pereyunamt = (String) data.get("pereyunamt");  // 개당이윤
    String eyunyul = (String) data.get("eyunyul");  // 이윤율

    String reqnum = (String) data.get("reqnum");  //주문번호
    boolean isNew = (reqnum == null || reqnum.isBlank());



    return ResponseEntity.ok().body(Map.of("success", true, "message", "주문 저장 완료"));
  }*/

  // 휴일 조회 메서드
  @GetMapping("/getHoliday")
  public AjaxResult getHoliday(){
    AjaxResult result = new AjaxResult();
    result.data = requestService.getHoliday();
    return result;
  }

}
