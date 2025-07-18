package mes.app.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.relational.core.sql.In;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
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
  private TB_DA006WFILERepository tbDa006WFILERepository;

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
      @RequestPart(value = "filelist", required = false) MultipartFile[] files,
      @RequestPart(value = "deletedFiles2", required = false) List<MultipartFile> deletedFiles2,
    Authentication auth
  )throws IOException {

    User user = (User) auth.getPrincipal();
    AjaxResult result = new AjaxResult();

    String custcd = (String) data.get("cboCompanyHidden");
    String spjangcd = (String) data.get("spjangcd");
    String rawReqdate = (String) data.get("reqdate");
    String reqdate = rawReqdate != null ? rawReqdate.replace("-", "") : null;
    String reqnum = (String) data.get("reqnum");

    boolean isNew = (reqnum == null || reqnum.isBlank());

    if (isNew) {
      // 🔹 신규일 경우 reqnum 채번
      reqnum = tbDa006WRepository.getNextReqnum(custcd, spjangcd, reqdate);
    }

    // ✅ 1. TB_DA006W 저장 (헤더)
    TB_DA006W_PK pk = new TB_DA006W_PK(custcd, spjangcd, reqdate, reqnum);
    TB_DA006W head;
    if (isNew) {
      head = new TB_DA006W();
    } else {
      head = tbDa006WRepository.findById(pk)
          .orElseThrow(() -> new RuntimeException("헤더 정보가 존재하지 않습니다."));
    }
    String deldate = String.valueOf(data.get("deldate"));
    if (deldate != null && deldate.length() == 10) {
      deldate = deldate.replace("-", "");
    }
    Object toteyunamtObj = data.get("toteyunamt");
    Object eyunyulObj = data.get("eyunyul");
    head.setId(pk);
    head.setCltcd(String.valueOf(data.get("cboCompanyHidden")));
    head.setCltnm((String) data.get("CompanyName"));  //회사이름

    head.setDeldate(deldate); //납기 희망일
    head.setCltzipcd(String.valueOf(data.get("cltzipcd"))); //우편번호
    head.setCltaddr(String.valueOf(data.get("address1")));  //업체 주소
    head.setModeltxt((String) data.get("product_code"));  //모델명
    head.setSetsamt(Long.parseLong((String) data.get("setsamt")));  //공급기준
    head.setSetqty(Long.parseLong((String) data.get("setqty")));    //수량
    head.setAmount(Long.parseLong((String) data.get("amount")));    //공급계
    head.setOutamt(Long.parseLong((String) data.get("outamt")));    //외주계
    head.setEyunamt(Long.parseLong((String) data.get("eyunamt")));  //이윤
    head.setPereyunamt(Long.parseLong((String) data.get("pereyunamt")));  //개당이윤율
    head.setOrdflag("0"); //0 : 주문의뢰 1:견적작성 2:제작 3:출고
    if (eyunyulObj != null && !eyunyulObj.toString().isBlank()) {
      head.setEyunyul(new BigDecimal(eyunyulObj.toString()));
    }  //이윤율
    if (toteyunamtObj != null) {
      head.setToteyunamt(new BigDecimal(toteyunamtObj.toString()));
    }
    head.setProjectno((String) data.get("projectno"));
    head.setInperid(user.getUsername());
    head.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    head.setTelno((String) data.get("telno"));
    tbDa006WRepository.save(head);
    log.info("헤더 저장 완료: {}", head);
    // ✅ 2. TB_DA007W 저장 (상세항목들)
    List<TB_DA007W> oldDetails = tbDa007WRepository.findById_CustcdAndId_SpjangcdAndId_ReqdateAndId_Reqnum(
        custcd, spjangcd, reqdate, reqnum);
    tbDa007WRepository.deleteAll(oldDetails);
    //tbDa007WRepository.deleteByPk(custcd, spjangcd, reqdate, reqnum);

    List<Map<String, Object>> detailList = (List<Map<String, Object>>) data.get("detailList");
    int seq = 1;
    for (Map<String, Object> row : detailList) {
      TB_DA007W_PK detailPk = new TB_DA007W_PK(custcd, spjangcd, reqdate, reqnum, String.format("%03d", seq++));
      TB_DA007W detail = new TB_DA007W();
      detail.setId(detailPk);
      detail.setModelnm((String) row.get("txtModelNm"));
      detail.setPname((String) row.get("pname"));
      detail.setJapcode((String) row.get("mat_code"));
      detail.setQty(Double.parseDouble((String) row.get("qty")));
      detail.setSetamt(Double.parseDouble((String) row.get("setamt")));
      detail.setSaleamt(Double.parseDouble((String) row.get("saleamt")));
      detail.setUamt(Double.parseDouble((String) row.get("uamt")));
      detail.setRemark((String) row.get("remark"));
      detail.setJobflag((String) row.get("jobflag"));
      detail.setInperid(user.getUsername());
      detail.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
      tbDa007WRepository.save(detail);
      log.info("상세 저장 완료: {}", detail);
    }

    // ✅ 3. 파일 저장
    String uploadPath = settings.getProperty("file_upload_path") + "주문등록" + File.separator + custcd + File.separator + reqnum;
    File uploadDir = new File(uploadPath);
    if (!uploadDir.exists()) uploadDir.mkdirs();

    if (files != null) {
      for (MultipartFile multipartFile : files) {
        String path = settings.getProperty("file_upload_path") + "주문등록";
        int fileSize = (int) multipartFile.getSize();

        if (fileSize > 52428800) {
          result.message = "파일의 크기가 초과하였습니다.";
          return ResponseEntity.badRequest().body(result);
        }

        String fileName = multipartFile.getOriginalFilename();
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String file_uuid_name = UUID.randomUUID().toString() + "." + ext;

        File saveDir = new File(path);
        if (!saveDir.isDirectory()) {
          saveDir.mkdirs();
        }
        File saveFile = new File(path + File.separator + file_uuid_name);
        multipartFile.transferTo(saveFile);

        TB_DA006WFile tbDa006WFile = new TB_DA006WFile();
        tbDa006WFile.setFilepath(path);
        tbDa006WFile.setFilesvnm(file_uuid_name);
        tbDa006WFile.setFileornm(fileName);
        tbDa006WFile.setFilesize(BigDecimal.valueOf(fileSize));
        tbDa006WFile.setFileextns(ext);
        tbDa006WFile.setFileurl(path);

        tbDa006WFile.setCustcd(custcd);
        tbDa006WFile.setSpjangcd(spjangcd);
        tbDa006WFile.setReqdate(reqdate);
        tbDa006WFile.setReqnum(reqnum);

        tbDa006WFile.setIndatem(reqdate);
        tbDa006WFile.setInuserid(user.getUsername());
        tbDa006WFile.setInusernm(user.getUsername());

        if (!requestService.saveFile(tbDa006WFile)) {
          result.success = false;
          result.message = "저장에 실패하였습니다.";
          break;
        }
      }
    }

    // ✅ 4. 파일 삭제
    if (deletedFiles2 != null && !deletedFiles2.isEmpty()) {
      List<TB_DA006WFile> tbDa006WFileList = new ArrayList<>();

      for (MultipartFile deletedFile : deletedFiles2) {
        String content = new String(deletedFile.getBytes(), StandardCharsets.UTF_8);
        Map<String, Object> deletedFileMap = new ObjectMapper().readValue(content, new TypeReference<>() {});
        Integer fileid = (Integer) deletedFileMap.get("fileid");

        TB_DA006WFile tbDa006WFile = tbDa006WFILERepository.findById(fileid).orElse(null);
        if (tbDa006WFile != null) {
          File file = new File(tbDa006WFile.getFilepath(), tbDa006WFile.getFilesvnm());
          if (file.exists()) {
            file.delete(); // 파일 삭제
          }
          tbDa006WFileList.add(tbDa006WFile); // 삭제 대상 목록에 추가
        }
      }

      // 🔹 DB 삭제
      tbDa006WFILERepository.deleteAll(tbDa006WFileList);
    }


    return ResponseEntity.ok(Map.of("success", true,
        "message", "주문 저장 완료", "reqnum", reqnum));
  }

  //tab2 read
  @GetMapping("/read")
  public AjaxResult getTab2Read(@RequestParam(value = "cboCompany", required = false) Integer compcd ,
                                @RequestParam(value = "ord_flag") String ordflag,
                                @RequestParam(value = "start") String start_date,
                                @RequestParam(value = "end") String end_date,
                                @RequestParam(value = "spjangcd") String spjangcd){
    AjaxResult result = new AjaxResult();
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";
    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> Tab2Read = requestService.getTab2Read(compcd, ordflag ,start, end, spjangcd);
    result.data = Tab2Read;
    return result;
  }
  // 휴일 조회 메서드
  @GetMapping("/getHoliday")
  public AjaxResult getHoliday(){
    AjaxResult result = new AjaxResult();
    result.data = requestService.getHoliday();
    return result;
  }

}
