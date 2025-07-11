/*
package mes.app.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.service.RequestService;
import mes.app.request.service.RequestService2;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/request/request2")
public class RequestController2 {

    @Autowired
    private RequestService2 requestService;

    @Autowired
    private TB_DA007WRepository tbda007WRepository;

    @Autowired
    private TB_DA006WRepository tbda006WRepository;

    @Autowired
    private TB_DA006WFILERepository tbDa006WFILERepository;

    @Autowired
    Settings settings;
    // 주문등록 세부사항 그리드 read
    @GetMapping("/read")
    public AjaxResult getList(@RequestParam Map<String, String> params,
                              Authentication auth) {
        String username = params.getOrDefault("saupnum", "").trim();

        //`saupnum`이 없으면 `saupnumHidden`을 가져오기
        if (username.isEmpty()) {
            username = params.getOrDefault("saupnumHidden", "").trim();
        }
        if (username.isEmpty()) {
            User user = (User) auth.getPrincipal();
            username = user.getUsername(); // 로그인한 사용자의 username 사용
        }

        Map<String, Object> userInfo = requestService.getUserInfo(username);
        if (userInfo == null) {
            System.out.println("⚠️ userInfo가 NULL입니다. username: " + username);
            throw new IllegalArgumentException("해당 사용자 정보를 찾을 수 없습니다: " + username);
        }
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setReqnum(params.get("reqnum"));
        tbDa006WPk.setReqdate(params.get("reqdate").replaceAll("-",""));
        tbDa006WPk.setSpjangcd((String) userInfo.get("spjangcd"));
        tbDa006WPk.setCustcd((String) userInfo.get("custcd"));
        List<Map<String, Object>> items = this.requestService.getInspecList(tbDa006WPk);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }
    // 주문의뢰현황 그리드 read
    @GetMapping("/order_read")
    public AjaxResult getOrderList(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                   @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                   @RequestParam(value = "search_remark", required = false) String searchRemark,
                                   @RequestParam(value = "search_ordflag", required = false) String searchOrdfalg,
                                   @RequestParam(value = "saupnumHidden", required = false) String saupnumHidden,
                                   @RequestParam(value = "saupnum", required = false) String Saupnum,
                                   @RequestParam(value = "search_comnm", required = false) String searchComnm,
                                   Authentication auth) {
        String username = (Saupnum != null && !Saupnum.isEmpty()) ? Saupnum : "";
        String saupnum;
        String cltcd;
        // username이 없을경우/session삭제후 진입(관리자 진입)
        if (username.isEmpty()) {
            username = (saupnumHidden != null && !saupnumHidden.isEmpty()) ? saupnumHidden : "";
        }
        if (username.isEmpty()) {
            User user = (User) auth.getPrincipal();
            username = user.getUsername();
        }

        Map<String, Object> userInfo = requestService.getUserInfo(username);

        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setSpjangcd((String) userInfo.get("spjangcd"));
        tbDa006WPk.setCustcd((String) userInfo.get("custcd"));
        // 관리자 진입시 사업자 번호 없이 업체명(searchComnm)만으로 조회
        if(saupnumHidden != null) {
            saupnum = null;
            cltcd = null;
        } else{
            saupnum = (String) userInfo.get("saupnum");
            cltcd = (String) userInfo.get("cltcd");
        }
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.requestService.getOrderList(tbDa006WPk,
                search_startDate, search_endDate, searchRemark, searchOrdfalg, saupnum, cltcd, searchComnm);
//        Map<String, Object> headitem = this.requestService.getHeadList(tbDa006WPk);
//        items.add(headitem);
        for (Map<String, Object> item : items) {
            if (item.get("ordflag").equals("0")) {
                item.remove("ordflag");
                item.put("ordflag", "주문의뢰");
            } else if (item.get("ordflag").equals("1")) {
                item.remove("ordflag");
                item.put("ordflag", "주문확인");
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

            ObjectMapper objectMapper = new ObjectMapper();
            if (item.get("hd_files") != null) {
                try {
                    // JSON 문자열을 List<Map<String, Object>>로 변환
                    List<Map<String, Object>> fileitems = objectMapper.readValue((String) item.get("hd_files"), new TypeReference<List<Map<String, Object>>>() {});

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

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    //신규등록
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping("/save")
    public AjaxResult saveOrder(@RequestParam Map<String, String> params,
                                @RequestParam(value = "filelist", required = false) MultipartFile[] files,
                                @RequestPart(value = "deletedFiles2", required = false) MultipartFile[] deletedFiles,
                                Authentication auth) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        // ✅ `saupnum` → `saupnumHidden` → `auth` 순으로 username 설정
        String username = params.getOrDefault("saupnum", "").trim();

        if (username.isEmpty()) {
            username = params.getOrDefault("saupnumHidden", "").trim();
        }
        if (username.isEmpty()) {
            username = user.getUsername();
        }
        Map<String, Object> userInfo = requestService.getUserInfo(username);

        TB_DA006W_PK headpk = new TB_DA006W_PK();
        headpk.setCustcd((String) userInfo.get("custcd"));
        headpk.setSpjangcd((String) userInfo.get("spjangcd"));
        headpk.setReqdate(params.get("reqdate").replaceAll("-",""));
        // findMaxReqnum 메서드가 최대 reqnum 값을 정수로 반환한다고 가정
        int maxReqnum = tbda006WRepository.findMaxReqnum((String) userInfo.get("custcd"), "ZZ", params.get("reqdate").replaceAll("-",""));

        // 증가된 reqnum을 4자리 문자열로 변환
        String nextReqnum = String.format("%04d", maxReqnum + 1);

        // params에 reqnum 값이 없으면 nextReqnum을 사용하고, 있으면 params 값을 그대로 사용
        String reqnum = params.get("reqnum").equals("") ? nextReqnum : params.get("reqnum");
        headpk.setReqnum(reqnum);

        if(files != null){
            for (MultipartFile multipartFile : files) {
                String path = settings.getProperty("file_upload_path") + "주문등록";
                MultipartFile file = multipartFile;
                int fileSize = (int) file.getSize();

                if(fileSize > 52428800){
                    result.message = "파일의 크기가 초과하였습니다.";
                    return result;
                }
                String fileName = file.getOriginalFilename();
                String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String file_uuid_name = UUID.randomUUID().toString() + "." + ext;
                String saveFilePath = path;
                File saveDir = new File(saveFilePath);



                //디렉토리 없으면 생성
                if(!saveDir.isDirectory()){
                    saveDir.mkdirs();
                }
                File saveFile = new File(path + File.separator + file_uuid_name);
                file.transferTo(saveFile);
                TB_DA006WFile tbDa006WFile = new TB_DA006WFile();

                tbDa006WFile.setFilepath(saveFilePath);
                tbDa006WFile.setFilesvnm(file_uuid_name);
                tbDa006WFile.setFileornm(fileName);
                tbDa006WFile.setFilesize(BigDecimal.valueOf(fileSize));
                tbDa006WFile.setCustcd((String) userInfo.get("custcd"));
                tbDa006WFile.setSpjangcd((String) userInfo.get("spjangcd"));
                tbDa006WFile.setReqdate(params.get("reqdate").replaceAll("-",""));
                tbDa006WFile.setReqnum(reqnum);
                tbDa006WFile.setIndatem(params.get("reqdate").replaceAll("-",""));
                tbDa006WFile.setInuserid(String.valueOf(user.getId()));
                tbDa006WFile.setInusernm(username);
                tbDa006WFile.setFileextns(ext);
                tbDa006WFile.setFileurl(saveFilePath);

                if (!requestService.saveFile(tbDa006WFile)) {
                    result.success = false;
                    result.message = "저장에 실패하였습니다.";
                    break;
                }
            }
        }
        // 삭제된 파일 처리
        if (deletedFiles != null && deletedFiles.length > 0) {
            List<TB_DA006WFile> tbDa006WFileList = new ArrayList<>();

            for (MultipartFile deletedFile : deletedFiles) {
                String content = new String(deletedFile.getBytes(), StandardCharsets.UTF_8);
                Map<String, Object> deletedFileMap = new ObjectMapper().readValue(content, new TypeReference<Map<String, Object>>() {});

                Integer fileid = (Integer) deletedFileMap.get("fileid");

                TB_DA006WFile tbDa006WFile = tbDa006WFILERepository.findById(fileid).orElse(null);
                // id : fileid
                if (tbDa006WFile != null) {
                    // 파일 삭제
                    String filePath = tbDa006WFile.getFilepath();
                    String fileName = tbDa006WFile.getFilesvnm();
                    File file = new File(filePath, fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    tbDa006WFileList.add(tbDa006WFile);
                }
            }
            tbDa006WFILERepository.deleteAll(tbDa006WFileList);
        }
        TB_DA006W tbDa006 = new TB_DA006W();

        tbDa006.setPk(headpk);
        tbDa006.setCltcd((String) userInfo.get("cltcd"));
        tbDa006.setCltnm((String) userInfo.get("cltnm"));
        tbDa006.setSaupnum((String) userInfo.get("saupnum"));

        tbDa006.setCltzipcd(params.get("postno"));
        tbDa006.setCltaddr(params.get("address1"));
        tbDa006.setDeldate(params.get("deldate").replaceAll("-",""));
        tbDa006.setPerid(params.get("perid"));      // 담당자명
        tbDa006.setPanel_ht(params.get("panel_ht"));
        tbDa006.setPanel_hw(params.get("panel_hw"));
        tbDa006.setPanel_hl(params.get("panel_hl"));
        tbDa006.setPanel_hh(params.get("panel_hh"));
        tbDa006.setIndate(params.get("deldate").replaceAll("-",""));
        tbDa006.setInperid(String.valueOf(user.getId()));
        tbDa006.setTelno(params.get("telno"));
        tbDa006.setRemark(params.get("remark"));
        //tbDa006.setOrdtext(now);

        boolean mainSaveSuccess = requestService.save(tbDa006);
        result.success = mainSaveSuccess;
        result.message = mainSaveSuccess ? "주문 정보 저장 성공" : "주문 정보 저장 실패";

        if (!mainSaveSuccess) return result; // 주문 정보 저장 실패 시 조기 종료

        // 'bodyData' 필드를 JSON 문자열로 받아오기
        try {
            String bodyDataJson = params.get("bodyData");
            if (bodyDataJson != null && !bodyDataJson.isEmpty()) {
                // JSON 배열을 List<Map<String, Object>>로 변환
                List<Map<String, Object>> jsonDataList = objectMapper.readValue(
                        bodyDataJson, new TypeReference<List<Map<String, Object>>>() {}
                );
                String custcd = (String) userInfo.get("custcd");
                String spjangcd = (String) userInfo.get("spjangcd");
                List<String> reqseqList = tbda007WRepository.findReqseq(reqnum, custcd, spjangcd);
                boolean allSuccessful = true;
                for (Map<String, Object> jsonData : jsonDataList) {
                    TB_DA007W tbDa007 = new TB_DA007W();
                    TB_DA007W_PK bodypk = new TB_DA007W_PK();
                    // Map을 통해 필드에 접근
                    bodypk.setCustcd((String) userInfo.get("custcd"));
                    bodypk.setSpjangcd((String) userInfo.get("spjangcd"));
                    bodypk.setReqdate(params.get("reqdate").replaceAll("-",""));
                    bodypk.setReqnum(reqnum);

                    tbDa007.setHgrb((String) jsonData.get("hgrb"));
                    tbDa007.setPanel_t((String) jsonData.get("panel_t"));
                    tbDa007.setPanel_w((String) jsonData.get("panel_w"));
                    tbDa007.setPanel_l((String) jsonData.get("panel_l"));
                    tbDa007.setQty((Integer) jsonData.get("qty"));
                    tbDa007.setExfmtypedv((String) jsonData.get("exfmtypedv"));
                    tbDa007.setInfmtypedv((String) jsonData.get("infmtypedv"));
                    tbDa007.setStframedv((String) jsonData.get("stframedv"));
                    tbDa007.setStexplydv((String) jsonData.get("stexplydv"));
                    tbDa007.setOrdtext((String) jsonData.get("ordtext"));
                    tbDa007.setIndate(params.get("deldate").replaceAll("-",""));
                    tbDa007.setInperid(String.valueOf(user.getId()));

                    // 기존 항목인지 확인하고 삽입 또는 업데이트
                    String reqseq = (String) jsonData.get("reqseq");
                    if (reqseqList.contains(reqseq)) {
                        bodypk.setReqseq(reqseq);
                        tbDa007.setPk(bodypk);

                        boolean successUpdate = requestService.updateBody(tbDa007);
                        allSuccessful &= successUpdate;
                        result.message += successUpdate ? " | 세부사항 업데이트 성공" : " | 세부사항 업데이트 실패";
                    } else {
                        reqseq = String.format("%03d",tbda007WRepository.findMaxReqseq(reqnum, (String) userInfo.get("custcd"), (String) userInfo.get("spjangcd")) + 1);
                        bodypk.setReqseq(reqseq);
                        tbDa007.setPk(bodypk);

                        boolean successInsert = requestService.saveBody(tbDa007);
                        reqseqList.add(reqseq); // 삽입 후 리스트에 추가
                        allSuccessful &= successInsert;
                        result.message += successInsert ? " | 세부사항 저장 성공" : " | 세부사항 저장 실패";
                    }
                }

                result.success = allSuccessful;
                result.message = "주문등록 되었습니다.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.success = false;
            result.message = "JSON 파싱 오류";
            return result;
        }

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
    //제품구성 리스트 불러오는 function
    @GetMapping("/getListHgrb")
    public AjaxResult getListHgrb(){
        List<Map<String, Object>> items = this.requestService.getListHgrb();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    //보강재, 마감재 리스트 불러오는 function
    @GetMapping("/getListCgrb")
    public AjaxResult getListCgrb(){
        List<Map<String, Object>> items = this.requestService.getListCgrb();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    // 유저정보 불러와 input태그 value
    @GetMapping("/getUserInfo")
    public AjaxResult getUserInfo(@RequestParam(value = "saupnum", required = false) String Saupnum,
                                  Authentication auth){
        String username;
        if (Saupnum != null && !Saupnum.isEmpty()) {
            username = Saupnum;
        } else {
            User user = (User) auth.getPrincipal();
            username = user.getUsername();
            System.out.println("username : " + username);
        }
        Map<String, Object> userInfo = requestService.getMyInfo(username);

        AjaxResult result = new AjaxResult();
        result.data = userInfo;
        return result;
    }
    @PostMapping("/uploadEditor")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String uploadDir = "c:\\temp\\editorFile\\";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File destinationFile = new File(uploadDir + fileName);
            file.transferTo(destinationFile);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String fileUrl = baseUrl + "/editorFile/" + fileName; // 클라이언트 접근 URL

            return ResponseEntity.ok(Collections.singletonMap("location", fileUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "파일 업로드 실패: " + e.getMessage()));
        }
    }
    // 삭제 메서드
    @PostMapping("/delete")
    public AjaxResult deleteHead(@RequestParam String reqnum,
                                 Authentication auth) {
        User user = (User) auth.getPrincipal();
        AjaxResult result = new AjaxResult();
        String username = user.getUsername();
        Map<String, Object> userInfo = requestService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setReqnum(reqnum);
        tbDa006WPk.setSpjangcd((String) userInfo.get("spjangcd"));
        tbDa006WPk.setCustcd((String) userInfo.get("custcd"));
        List<Map<String, Object>> items = this.requestService.getInspecList(tbDa006WPk);
        List<String> imageUrls = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String ordtext = (String) item.get("ordtext"); // 큰따옴표로 수정
            if (ordtext != null) {
                imageUrls.addAll(extractImageUrlsFromHtml(ordtext)); // extractImageUrlsFromHtml 결과를 추가
            }
        }
        // 3. 이미지 삭제 처리
        boolean allDeleted = true;
        for (String imageUrl : imageUrls) {
            boolean deleted = deleteImageFromServer(imageUrl); // 이미지 삭제 메서드 호출
            if (!deleted) {
                allDeleted = false;
                // 로그에 실패한 이미지 정보 기록
                System.err.println("이미지 삭제 실패: " + imageUrl);
            }
        }
        boolean success = requestService.delete(reqnum);

        if (success && allDeleted) {
            result.success = true;
            result.message = "삭제하였습니다.";
        } else {
            result.success = false;
            result.message = "삭제에 실패하였습니다.";
        }
        return result;
    }
    // body 삭제 메서드
    @PostMapping("/bodyDelete")
    public AjaxResult deleteBody(@RequestParam Map<Object, String> param,
                                 Authentication auth) {
        AjaxResult result = new AjaxResult();

        String username = param.getOrDefault("saupnum", "").trim();

        //`saupnum`이 없으면 `saupnumHidden`을 가져오기
        if (username.isEmpty()) {
            username = param.getOrDefault("saupnumHidden", "").trim();
        }
        if (username.isEmpty()) {
            User user = (User) auth.getPrincipal();
            username = user.getUsername(); // 로그인한 사용자의 username 사용
        }
        Map<String, Object> userInfo = requestService.getUserInfo(username);
        TB_DA006W_PK tbDa006WPk = new TB_DA006W_PK();
        tbDa006WPk.setReqnum(param.get("reqnum"));
        tbDa006WPk.setSpjangcd((String) userInfo.get("spjangcd"));
        tbDa006WPk.setCustcd((String) userInfo.get("custcd"));
        String reqseq = param.get("reqseq");
        String reqnum = param.get("reqnum");
        String reqdate = param.get("reqdate");

        Map<String, Object> item = this.requestService.getInspecList2(tbDa006WPk, reqseq, reqdate);
        List<String> imageUrls = new ArrayList<>();
        String ordtext = (String) item.get("ordtext"); // 큰따옴표로 수정
        if (ordtext != null) {
            imageUrls.addAll(extractImageUrlsFromHtml(ordtext)); // extractImageUrlsFromHtml 결과를 추가
        }
        // 3. 이미지 삭제 처리
        boolean allDeleted = true;
        for (String imageUrl : imageUrls) {
            boolean deleted = deleteImageFromServer(imageUrl); // 이미지 삭제 메서드 호출
            if (!deleted) {
                allDeleted = false;
                // 로그에 실패한 이미지 정보 기록
                System.err.println("이미지 삭제 실패: " + imageUrl);
            }
        }
        boolean success = requestService.deleteBody(reqseq, reqnum);

        if (success && allDeleted) {
            result.success = true;
            result.message = "삭제하였습니다.";
        } else {
            result.success = false;
            result.message = "삭제에 실패하였습니다.";
        }
        return result;
    }
    // 에디터 파일 파싱 메서드
    private List<String> extractImageUrlsFromHtml(String htmlContent) {
        List<String> imageUrls = new ArrayList<>();

        try {
            // JSoup 라이브러리를 사용해 HTML 파싱
            Document doc = Jsoup.parse(htmlContent);
            Elements images = doc.select("img"); // <img> 태그 선택

            for (Element img : images) {
                String src = img.attr("src"); // src 속성 값 추출
                if (src != null && !src.isEmpty()) {
                    imageUrls.add(src);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageUrls;
    }
    // 에디터 파일 삭제 메서드
    private boolean deleteImageFromServer(String imageUrl) {
        try {
            // 이미지 URL에서 파일 경로 추출
            String uploadDir = "c:/temp/editorFile/"; // 업로드된 파일이 저장된 디렉토리
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1); // 파일 이름 추출
            File file = new File(uploadDir + fileName);

            // 파일 존재 여부 확인 후 삭제
            if (file.exists()) {
                return file.delete();
            } else {
                System.err.println("삭제할 파일이 존재하지 않습니다: " + file.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 휴일 조회 메서드
    @GetMapping("/getHoliday")
    public AjaxResult getHoliday(){
        AjaxResult result = new AjaxResult();
        result.data = requestService.getHoliday();
        return result;
    }

}

*/
