package mes.app.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import mes.app.MailService;
import mes.app.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.model.AjaxResult;
import mes.domain.model.CopyResult;
import mes.domain.repository.actasRepository.ModelHistoryRepository;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/request/request")
public class RequestController {

  @Autowired
  RequestService requestService;

  @Autowired
  private TB_DA007WRepository tbDa007WRepository;

  @Autowired
  private TB_DA006WRepository tbDa006WRepository;

  @Autowired
  private TB_DA006WFILERepository tbDa006WFILERepository;

  @Autowired
  ModelHistoryRepository modelHistoryRepository;

  @Autowired
  MailService mailService;

  @Autowired
  Settings settings;

  //bom 리스트 read
  @GetMapping("/bomList")
  public AjaxResult getBomList(@RequestParam(value = "Material_id") Integer Material_id) {
    AjaxResult result = new AjaxResult();
    List<Map<String, Object>> bomList = requestService.getBomList(Material_id);
    if (bomList.isEmpty()) {
      result.success = false;
      result.message = "BOM 정보가 없습니다.";
      return result;
    }
    Integer bomId = (Integer) bomList.get(0).get("id");

    List<Map<String, Object>> bomTree = requestService.getBomComponentTreeList(bomId, "ZZ");
    result.data = bomTree;
    return result;
  }

  @PostMapping("/copyData")
  public AjaxResult copyData(@RequestBody Map<String, Object> payload, Authentication auth) {
    AjaxResult res = new AjaxResult();
    User user = (User) auth.getPrincipal();

    try {
      // 1) reqnums 배열 파싱
      Object obj = payload.get("reqnums");
      List<String> oldReqnums = (obj instanceof List<?> list)
          ? list.stream().map(String::valueOf).toList()
          : List.of();

      if (oldReqnums.isEmpty()) {
        res.success = false;
        res.code = "VALIDATION_ERROR";
        res.StateName = "FAIL";
        res.message = "reqnums가 비어 있습니다.";
        res.data = null;
        return res;
      }

      // 2) 선택적 reqdate (없으면 서비스에서 원본 reqdate 사용)
      String overrideReqdate = (String) payload.get("reqdate");

      String spjangcd = user.getSpjangcd();
      String actor    = user.getUsername(); // 또는 사용자 ID

      // 3) 순차 복제 실행(입력 순서대로 처리)
      CopyResult r = requestService.copyOrdersSequential(oldReqnums, spjangcd, overrideReqdate, actor);

      // 4) 응답 바디(data) 구성
      Map<String, Object> data = new HashMap<>();
      data.put("copied", r.copied());
      data.put("skipped", r.skipped());
      data.put("newReqnums", r.newReqnums());
      data.put("failures", r.failures()); // 부분성공 시 실패 사유 리스트

      // 5) AjaxResult 필드 세팅
      res.success = (r.skipped() == 0);
      res.code = (r.skipped() == 0) ? "OK" : "PARTIAL_SUCCESS";
      res.StateName = (r.skipped() == 0) ? "SUCCESS" : "PARTIAL";
      res.message = (r.skipped() == 0)
          ? "복사 완료"
          : String.format("일부만 복사되었습니다. 성공 %d건, 실패 %d건", r.copied(), r.skipped());
      res.data = data;

      return res;
    } catch (Exception e) {
      // 예외 처리
      res.success = false;
      res.code = "SERVER_ERROR";
      res.StateName = "FAIL";
      res.message = "복사 처리 중 오류가 발생했습니다: " + e.getMessage();
      res.data = null;
      return res;
    }
  }

  @PostMapping("/saveWithFiles")
  @Transactional
  public ResponseEntity<?> saveOrderWithFiles(
      @RequestPart("jsonData") Map<String, Object> data,
      @RequestPart(value = "filelist", required = false) MultipartFile[] files,
      @RequestPart(value = "deletedFiles2", required = false) List<MultipartFile> deletedFiles2,
      Authentication auth
  ) throws IOException {

    User user = (User) auth.getPrincipal();
    AjaxResult result = new AjaxResult();

    String custcd = (String) data.get("cboCompanyHidden");
    String spjangcd = (String) data.get("spjangcd");
    String rawReqdate = (String) data.get("reqdate");
    String reqdate = rawReqdate != null ? rawReqdate.replace("-", "") : null;
    String reqnum = (String) data.get("reqnum");
    String modeltxt_history = (String) data.get("modeltxt_history");

    boolean isNew = (reqnum == null || reqnum.isBlank());

    if (isNew) {
      // 🔹 신규일 경우 reqnum 채번
      reqnum = tbDa006WRepository.getNextReqnum(spjangcd);
    }

    // ✅ 1. TB_DA006W 저장 (헤더)
    TB_DA006W_PK pk = new TB_DA006W_PK(custcd, spjangcd, reqdate, reqnum);
//    log.debug("custcd={}, spjangcd={}, reqdate={}, reqnum={}", custcd, spjangcd, reqdate, reqnum);

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
    head.setPerid(String.valueOf(data.get("perid"))); //담당자
    head.setOperid(String.valueOf(data.get("perid"))); //발주담당
    head.setCltzipcd(String.valueOf(data.get("postno"))); //우편번호
    head.setCltaddr(String.valueOf(data.get("address1")));  //업체 주소
    head.setPcode(Long.parseLong((String) data.get("product_code")));//모델코드
    head.setModeltxt((String) data.get("modeltxt"));  //모델명
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

    // ✅ 1-1. 모델 이력 저장
    if (modeltxt_history != null && !modeltxt_history.isBlank()) {
      Long modelid = head.getPcode();

      // 🔍 최신 version_no 조회
      Integer lastVersion = modelHistoryRepository
          .findMaxVersionNoByModelid(String.valueOf(modelid))
          .orElse(0);  // 없으면 0

      ModelHistory history = new ModelHistory();
      history.setModelid(String.valueOf(modelid));
      history.setCustcd(custcd);
      history.setSpjangcd(spjangcd);
      history.setReqdate(reqdate);
      history.setReqnum(reqnum);
      history.setCltcd(head.getCltcd());
      history.setCltnm(head.getCltnm());
      history.setPrev_modeltxt(modeltxt_history);  // 🔄 이전 모델 설명
      history.setModeltxt_current(data.get("modeltxt_history").toString()); // 현재 모델 설명
      history.setVersion_no(lastVersion + 1);
      history.setChange_date(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
      history.setChanger_name(user.getUsername());

      modelHistoryRepository.save(history);
      log.info("모델 이력 저장 완료: {}", history);
    }

    // ✅ 2. TB_DA007W 저장 (상세항목들)
    List<TB_DA007W> oldDetails = tbDa007WRepository.findById_CustcdAndId_SpjangcdAndId_ReqdateAndId_Reqnum(
        custcd, spjangcd, reqdate, reqnum);
    tbDa007WRepository.deleteAll(oldDetails);

    List<Map<String, Object>> detailList = (List<Map<String, Object>>) data.get("detailList");
    int seq = 1;

    for (Map<String, Object> row : detailList) {
      Long modelId = head.getPcode();
      String modelname = head.getModeltxt();
      TB_DA007W_PK detailPk = new TB_DA007W_PK(custcd, spjangcd, reqdate, reqnum, String.format("%03d", seq++));
      TB_DA007W detail = new TB_DA007W();
      detail.setId(detailPk);
      detail.setModelnm(modelname);
      detail.setPcode(Long.valueOf(modelId)); //모델 코드
      detail.setPname((String) row.get("pname"));
      detail.setJapcode(parseBigDecimalSafe(row.get("mat_code")));
      detail.setQty(parseDoubleSafe(row.get("qty")));
      detail.setSetamt(parseBigDecimalSafe(row.get("setamt")));
      detail.setSaleamt(parseBigDecimalSafe(row.get("saleamt")));
      detail.setUamt(parseDoubleSafe(row.get("uamt")));
      detail.setRemark((String) row.get("remark"));
      detail.setClttype((String) row.get("clttype"));
      detail.setJobflag((String) row.get("jobflag"));
      detail.setInperid(user.getUsername());
      detail.setIndate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
      tbDa007WRepository.save(detail);
//      log.info("상세 저장 완료: {}", detail);
    }

    // ✅ 3. 파일 저장
    String uploadPath = settings.getProperty("file_upload_path") + "주문등록" + File.separator + custcd + File.separator + reqnum;
    File uploadDir = new File(uploadPath);
    if (!uploadDir.exists()) uploadDir.mkdirs();

    if (files != null) {
      for (MultipartFile multipartFile : files) {
        String path = settings.getProperty("file_upload_path") + "주문등록";
        String originalFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize(); // ✅ 수정 완료
        log.info("업로드 시도 - 파일명: {}, 크기: {} bytes", originalFilename, fileSize);

        if (fileSize > 52428800L) {
          result.message = "파일의 크기가 초과하였습니다.";
          log.info("파일 크기 초과 - 파일명: {}, 크기: {} bytes", originalFilename, fileSize);
          return ResponseEntity.badRequest().body(result);
        }

        String fileName = originalFilename;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.parse(reqdate, formatter);
        Timestamp timestamp = Timestamp.valueOf(localDate.atStartOfDay());
        tbDa006WFile.setIndatem(timestamp);

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
    if (!isNew && deletedFiles2 != null && !deletedFiles2.isEmpty()) {
      List<TB_DA006WFile> tbDa006WFileList = new ArrayList<>();

      for (MultipartFile deletedFile : deletedFiles2) {
        String content = new String(deletedFile.getBytes(), StandardCharsets.UTF_8);
        Map<String, Object> deletedFileMap = new ObjectMapper().readValue(content, new TypeReference<>() {
        });

        Object fileidObj = deletedFileMap.get("fileid");
        if (fileidObj == null) {
          log.warn("fileid 누락: {}", deletedFileMap);
          continue;
        }

        Integer fileid;
        try {
          fileid = Integer.parseInt(fileidObj.toString());
        } catch (NumberFormatException e) {
          log.warn("fileid 파싱 실패: {}", fileidObj);
          continue;
        }

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

  private static Double parseDoubleSafe(Object val) {
    if (val == null) return null;
    String clean = val.toString().trim().replace(",", "");
    if (clean.isEmpty()) return null;
    try {
      return Double.parseDouble(clean);
    } catch (NumberFormatException e) {
      System.err.println("Double 변환 실패: [" + val + "]");
      return null;
    }
  }

  //tab2 read
  @GetMapping("/read")
  public AjaxResult getTab2Read(@RequestParam(value = "cboCompany2", required = false) Integer compcd,
                                @RequestParam(value = "company_name", required = false) String company_name,
                                @RequestParam(value = "ord_flag") String ordflag,
                                @RequestParam(value = "start") String start_date,
                                @RequestParam(value = "end") String end_date,
                                @RequestParam(value = "spjangcd") String spjangcd) {
    AjaxResult result = new AjaxResult();
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";
    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    /*log.info("tab2 read :cboCompany:{},company_name:{},ordflag:{}, start_date:{},end_date:{},spjangcd:{}",
        compcd, company_name, ordflag, start_date, end_date, spjangcd);*/

    List<Map<String, Object>> Tab2Read = requestService.getTab2Read(compcd, company_name, ordflag, start, end, spjangcd);
    for (Map<String, Object> item : Tab2Read) {
      ObjectMapper objectMapper = new ObjectMapper();
      if (item.get("hd_files") != null) {
        try {
          // JSON 문자열을 List<Map<String, Object>>로 변환
          List<Map<String, Object>> fileitems = objectMapper.readValue((String) item.get("hd_files"), new TypeReference<List<Map<String, Object>>>() {
          });

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

          // fileitems를 다시 item에 넣어 업데이트
          item.remove("hd_files");
          item.put("hd_files", fileitems);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    result.data = Tab2Read;
    return result;
  }

  // 휴일 조회 메서드
  @GetMapping("/getHoliday")
  public AjaxResult getHoliday() {
    AjaxResult result = new AjaxResult();
    result.data = requestService.getHoliday();
    return result;
  }

  @PostMapping("/downloader")
  public ResponseEntity<?> downloadFile(@RequestBody List<Map<String, Object>> reqnums) throws IOException {

    // 파일 목록과 파일 이름을 담을 리스트 초기화
    List<File> filesToDownload = new ArrayList<>();
    List<String> fileNames = new ArrayList<>();

    // ZIP 파일 이름을 설정할 변수 초기화
    String tketcrdtm = null;
    String tketnm = null;

    // 파일을 메모리에 쓰기
    for (Map<String, Object> reqnum : reqnums) {
      // 다운로드 위한 파일 정보 조회
      List<Map<String, Object>> fileList = requestService.download(reqnum);

      for (Map<String, Object> fileInfo : fileList) {
        String filePath = (String) fileInfo.get("filepath");    // 파일 경로
        String fileName = (String) fileInfo.get("filesvnm");    // 파일 이름(uuid)
        String originFileName = (String) fileInfo.get("fileornm");  //파일 원본이름(origin Name)

        if (tketcrdtm == null) {
          tketcrdtm = (String) fileInfo.get("reqdate");
        }
        if (tketnm == null) {
          tketnm = "주문등록";
        }

        File file = new File(filePath + File.separator + fileName);

        // 파일이 실제로 존재하는지 확인
        if (file.exists()) {
          filesToDownload.add(file);
          fileNames.add(originFileName); // 다운로드 받을 파일 이름을 originFileName으로 설정
        }
      }
    }

    // 파일이 없는 경우
    if (filesToDownload.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    // 파일이 하나인 경우 그 파일을 바로 다운로드
    if (filesToDownload.size() == 1) {
      File file = filesToDownload.get(0);
      String originFileName = fileNames.get(0); // originFileName 가져오기

      HttpHeaders headers = new HttpHeaders();
      String encodedFileName = URLEncoder.encode(originFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedFileName);
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentLength(file.length());

      ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file.toPath()));

      return ResponseEntity.ok()
          .headers(headers)
          .body(resource);
    }

    String zipFileName = (tketcrdtm != null && tketnm != null) ? tketcrdtm + "_" + tketnm + ".zip" : "download.zip";

    // 파일이 두 개 이상인 경우 ZIP 파일로 묶어서 다운로드
    ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
    try (ZipOutputStream zipOut = new ZipOutputStream(zipBaos)) {

      Set<String> addedFileNames = new HashSet<>(); // 이미 추가된 파일 이름을 저장할 Set
      int fileCount = 1;

      for (int i = 0; i < filesToDownload.size(); i++) {
        File file = filesToDownload.get(i);
        String originFileName = fileNames.get(i); // originFileName 가져오기

        // 파일 이름이 중복될 경우 숫자를 붙여 고유한 이름으로 만듦
        String uniqueFileName = originFileName;
        while (addedFileNames.contains(uniqueFileName)) {
          uniqueFileName = originFileName.replace(".", "_" + fileCount++ + ".");
        }

        // 고유한 파일 이름을 Set에 추가
        addedFileNames.add(uniqueFileName);

        try (FileInputStream fis = new FileInputStream(file)) {
          ZipEntry zipEntry = new ZipEntry(originFileName);
          zipOut.putNextEntry(zipEntry);

          byte[] buffer = new byte[1024];
          int len;
          while ((len = fis.read(buffer)) > 0) {
            zipOut.write(buffer, 0, len);
          }

          zipOut.closeEntry();
        } catch (IOException e) {
          e.printStackTrace();
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
      }

      zipOut.finish();
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    ByteArrayResource zipResource = new ByteArrayResource(zipBaos.toByteArray());

    HttpHeaders headers = new HttpHeaders();
    String encodedZipFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedZipFileName);
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentLength(zipResource.contentLength());

    return ResponseEntity.ok()
        .headers(headers)
        .body(zipResource);
  }

  @GetMapping("/getDetailList")
  public AjaxResult getOrderDetail(
      @RequestParam("spjangcd") String spjangcd,
      @RequestParam("reqnum") String reqnum,
      HttpServletRequest request) {


//    log.info("getDetailList 들어옴 spjangcd:{},  reqnum:{}", spjangcd,  reqnum);
    List<Map<String, Object>> detailList = requestService.getOrderDetail(reqnum, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = detailList;

    return result;
  }

  @PostMapping("/delete")
  @Transactional
  public AjaxResult deleteOrderData(@RequestParam("id") String reqnum) {
    AjaxResult result = new AjaxResult();

    try {

      // 🔍 헤더 정보 조회
      TB_DA006W head = tbDa006WRepository.findByReqnum(reqnum)
          .orElseThrow(() -> new RuntimeException("해당 reqnum에 대한 주문 헤더가 없습니다."));

      String custcd = head.getId().getCustcd();
      String spjangcd = head.getId().getSpjangcd();
      String reqdate = head.getId().getReqdate();
      String modeltxt = head.getModeltxt();

      // 🔽 파일 삭제 (물리 + DB)
      List<TB_DA006WFile> files = tbDa006WFILERepository.findAllByReqnum(reqnum);
      for (TB_DA006WFile file : files) {
        File physicalFile = new File(file.getFilepath(), file.getFilesvnm());
        if (physicalFile.exists()) physicalFile.delete();
        tbDa006WFILERepository.deleteById(file.getFileid());
      }

      // 🔽 모델 이력 삭제
      modelHistoryRepository.deleteByModelHistoryKey(modeltxt, custcd, spjangcd, reqdate, reqnum);

      // 🔽 상세/헤더 삭제
      tbDa007WRepository.deleteByPk(custcd, spjangcd, reqdate, reqnum);
      tbDa006WRepository.deleteByPk(custcd, spjangcd, reqdate, reqnum);

      result.success = true;
      result.message = "삭제가 완료되었습니다.";
    } catch (Exception e) {
      result.success = false;
      result.message = "삭제 중 오류가 발생했습니다: " + e.getMessage();
      log.error("❌ 주문 삭제 실패", e);
      throw e;
    }

    return result;
  }


  private static BigDecimal parseBigDecimalSafe(Object val) {
    if (val == null) return null;
    String clean = val.toString().trim().replace(",", "");
    if (clean.isEmpty()) return null;
    try {
      return new BigDecimal(clean);
    } catch (NumberFormatException e) {
      System.err.println("BigDecimal 변환 실패: [" + val + "]");
      return null;
    }
  }

  @PostMapping("/sendBalJuMail")
  public AjaxResult getMailData(@RequestBody Map<String, Object> payload, Authentication auth) {
    AjaxResult result = new AjaxResult();

    try {
      // 1. 요청 데이터 추출
      List<String> recipients = (List<String>) payload.get("recipients");
      String title = (String) payload.get("title");
      String content = (String) payload.get("content");
      String reqnum = (String) payload.get("reqnum");

      // 2. 로그인 사용자 정보
      User user = (User) auth.getPrincipal();
      String userid = user.getUsername();

      // 3. 발주서 데이터 조회
      Map<String, Object> mailDeta = requestService.getOrderMailDeta(reqnum);

      // 4. 엑셀 생성
      String projectNo = String.valueOf(mailDeta.get("projectno")).replaceAll("[\\\\/:*?\"<>|]", "");
      String fileName = String.format("_%s.xlsx", projectNo);
      Path tempXlsx = Paths.get("C:/Temp/mes21/문서/제품견적서" + fileName);
      Files.createDirectories(tempXlsx.getParent());
      Files.deleteIfExists(tempXlsx);

      String templatePath = "C:/Temp/mes21/문서/JNJBaljuTemplate.xlsx";

      try (FileInputStream fis = new FileInputStream(templatePath);
           Workbook workbook = new XSSFWorkbook(fis);
           FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

        Sheet sheet = workbook.getSheetAt(0);

        // 엑셀 바인딩
        setCell(sheet, 6, 4, String.valueOf(mailDeta.get("cltnm")));
        setCell(sheet, 7, 4, String.valueOf(mailDeta.get("deldate")));
        String rawDate = String.valueOf(mailDeta.get("reqdate"));
        String formattedDate = "";
        if (rawDate != null && rawDate.length() == 8) {
          String year = rawDate.substring(0, 4);
          String month = rawDate.substring(4, 6);
          String day = rawDate.substring(6, 8);
          formattedDate = year + "년 " + month + "월 " + day + "일";
        }

        setCell(sheet, 9, 5, String.valueOf(mailDeta.get("projectno")));
        setCell(sheet, 10, 4, formattedDate);
        setCell(sheet, 11, 4, String.valueOf(mailDeta.get("cltnm")));
        setCell(sheet, 12, 4, String.valueOf(mailDeta.get("perid")));

        setCell(sheet, 16, 2, String.valueOf(mailDeta.get("modeltxt")));
        setCell(sheet, 16, 9, String.valueOf(mailDeta.get("setqty")));
        setCell(sheet, 16, 18, String.valueOf(mailDeta.get("setsamt")));

        workbook.write(fos);
      } catch (Exception e) {
        log.error("❌ 엑셀 생성 중 오류 발생", e);
        result.success = false;
        result.message = "엑셀 생성 중 오류가 발생했습니다.";
        return result;
      }

      // 5. 메일 전송
      /*mailService.sendMailWithAttachment(
          recipients,
          title,
          content,
          tempXlsx.toFile(),
          fileName
      );*/

      // 6. 엑셀 다운로드 URL 구성
      String encodedFileName = org.springframework.web.util.UriUtils.encodePathSegment("제품견적서_" + projectNo + ".xlsx", StandardCharsets.UTF_8);
      String downloadUrl = "/baljuFile/" + encodedFileName;


      // 7. 파일 삭제 예약 (선택)
      Executors.newSingleThreadScheduledExecutor().schedule(() -> {
        try { Files.deleteIfExists(tempXlsx); } catch (IOException e) { e.printStackTrace(); }
      }, 5, TimeUnit.MINUTES);

      // 8. 결과 응답
      result.success = true;
      result.message = "메일이 성공적으로 전송되었습니다.";
      result.data = Map.of(
          "downloadUrl", downloadUrl,
          "fileName", fileName
      );
      return result;

    } catch (Exception e) {
      log.error("❌ 메일 전송 중 서버에서 예외 발생: {}", e.getMessage(), e);
      result.success = false;
      result.message = "메일 전송 중 문제가 발생했습니다: " + e.getMessage();
      return result;
    }
  }


  private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) row = sheet.createRow(rowIdx);
    Cell cell = row.getCell(colIdx);
    if (cell == null) cell = row.createCell(colIdx);
    cell.setCellValue(value);
  }

  @PostMapping("/SaveUnitPrice")
  public AjaxResult SaveUnitPrice(@RequestParam(value = "pcode")Integer pcode,
                                  @RequestParam(value = "pname")String pname,
                                  @RequestParam(value = "puamt") String puamt,
                                  @RequestParam(value = "cltcd") String cltcd){
    AjaxResult result = new AjaxResult();
//    log.info("주문등록 단가 저장 들어옴 pcode:{},pname:{}, puamt:{}, cltcd:{} ", pcode, pname, puamt, cltcd);
    try {
      String inputDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      int affectedRows = requestService.SaveUnitPrice(pcode, pname, puamt, cltcd, inputDate);

      if (affectedRows > 0) {
        result.success = true;
        result.message = "단가 저장 완료";
      } else {
        result.success = false;
        result.message = "저장할 데이터가 없습니다.";
      }
    } catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }
    return result;
  }

  @PostMapping("/savePrice")
  public AjaxResult mat_savePrice(@RequestParam(value = "mat_id")Integer pcode,
                                  @RequestParam(value = "pname")String pname,
                                  @RequestParam(value = "puamt") String puamt,
                                  @RequestParam(value = "cltcd") String cltcd){
    AjaxResult result = new AjaxResult();
    log.info("단가 저장 들어옴 pcode:{},pname:{}, puamt:{}, cltcd:{} ", pcode, pname, puamt, cltcd);
    try {
      String inputDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      int affectedRows = requestService.SaveUnitPrice(pcode, pname, puamt, cltcd, inputDate);

      if (affectedRows > 0) {
        result.success = true;
        result.message = "단가 저장 완료";
      } else {
        result.success = false;
        result.message = "저장할 데이터가 없습니다.";
      }
    } catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }
    return result;
  }
}
