package mes.app.actas_inspec.service;


import mes.config.Settings;
import mes.domain.DTO.TB_RP810Dto;
import mes.domain.entity.actasEntity.TB_RP810;
import mes.domain.entity.actasEntity.TB_RP815;
import mes.domain.repository.actasRepository.TB_RP815Repository;
import mes.domain.repository.actasRepository.TB_RP810Repository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class FieldService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    TB_RP810Repository tbRp810Repository;
    @Autowired
    TB_RP815Repository tbRp815Repository;


    @Autowired
    FileUploaderService fileService;

    @Autowired
    Settings settings;

    @PersistenceContext
    private EntityManager entityManager;


    public List<Map<String, Object>> getPRresponseList(){

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                    select a.id, a.username, a.first_name from auth_user as a
                          left join user_profile as b 
                          on a.id = b."User_id"
                          where
                          b."UserGroup_id" = '7' 
                """;
        return this.sqlRunner.getRows(sql, dicParam);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean save(TB_RP810Dto dto, MultipartFile[] files){

        try{
            TB_RP810 entity =  new TB_RP810Dto().toEntity(dto);

            tbRp810Repository.save(entity);

            Map<String, Object> metaItem = new HashMap<>();

            metaItem.put("spworkcd", dto.getSpworkcd());
            metaItem.put("spworknm", dto.getSpworknm());
            metaItem.put("spcompcd", dto.getSpcompcd());
            metaItem.put("spcompnm", dto.getSpcompnm());
            metaItem.put("spplancd", dto.getSpplancd());
            metaItem.put("spplannm", dto.getSpplannm());
            metaItem.put("spuncode", dto.getSpuncode());
            metaItem.put("inuserid", dto.getInuserid());
            metaItem.put("inusernm", dto.getInusernm());

            String path = settings.getProperty("file_upload_path") + "FieldService첨부파일";


            if(files != null){

                List<TB_RP815> entityList = new ArrayList<>();

                Optional<String> checkseqvalue = tbRp815Repository.findMaxChecknoByCheckdt(dto.getSpuncode());

                int checknointValue;

                checknointValue = checkseqvalue.map(Integer::parseInt).orElse(0);


                for (MultipartFile filelist: files){

                    ++checknointValue;

                    Map<String, Object> fileinform =  fileService.saveFiles(filelist, path); //DISK 저장

                    entityList.add(TB_RP815_Save(metaItem, fileinform, "N", checknointValue));  //DB 저장
                }

                tbRp815Repository.saveAll(entityList);

            }


            return true;
        }catch (Exception e){
            System.err.println("Error1 : " + e.getMessage());
            return false;
        }
    }

    public TB_RP815 TB_RP815_Save(Map<String, Object> metaItem, Map<String, Object> fileinform, String repyn, Integer checkseq){

        String spuncode = metaItem.get("spuncode").toString();


        TB_RP815 attachedFile = new TB_RP815();

        List<Map<String, Object>> fileItem = getFileList(spuncode);

        String fileName = (String) fileinform.get("fileName");

        for(Map<String, Object> item : fileItem){
            String fileOrNm = (String) item.get("fileornm");
            if(fileName.equals(fileOrNm)){
                return null;
            }
        }

        String formattedFileValue = String.format("%02d", checkseq);


        attachedFile.setSpworkcd(metaItem.get("spworkcd").toString());
        attachedFile.setSpcompcd(metaItem.get("spcompcd").toString());
        attachedFile.setSpplancd(metaItem.get("spplancd").toString());
        attachedFile.setSpuncode_id(spuncode);
        attachedFile.setSpworknm(metaItem.get("spworknm").toString());
        attachedFile.setSpcompnm(metaItem.get("spcompnm").toString());
        attachedFile.setSpplannm(metaItem.get("spplannm").toString());
        attachedFile.setCheckseq(formattedFileValue);
        attachedFile.setFilepath(fileinform.get("saveFilePath").toString());
        attachedFile.setFilesvnm(fileinform.get("file_uuid_name").toString());
        attachedFile.setFileextns(fileinform.get("ext").toString());
        attachedFile.setFileornm(fileinform.get("fileName").toString());
        attachedFile.setFilesize((Float) fileinform.get("fileSize"));
        attachedFile.setRepyn(repyn);
        attachedFile.setInuserid(metaItem.get("inuserid").toString());
        attachedFile.setInusernm(metaItem.get("inusernm").toString());

        //tbRp815Repository.save(attachedFile);
        return attachedFile;
    }

    public List<Map<String, Object>> getFileList(String SpunCode){

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("spuncode", SpunCode);

        String sql = """
                    select filepath, filesvnm
                    from tb_rp815
                    where "spuncode_id" = :spuncode
                    order by checkseq
                """;
        return this.sqlRunner.getRows(sql, dicParam);
    }


    public List<Map<String, Object>> getList(String searchfrdate, String searchtodate,
                                             String searchsitename, String searchesname) throws ParseException {
        // SimpleDateFormat을 사용하여 문자열을 Timestamp로 변환
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Timestamp fromDate = new Timestamp(dateFormat.parse(searchfrdate).getTime());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate searchToDate = LocalDate.parse(searchtodate, formatter);
// 해당 날짜의 23:59:59 시간으로 변환
        LocalDateTime endOfDay = searchToDate.atTime(23, 59, 59);
// Timestamp로 변환
        Timestamp toDate = Timestamp.valueOf(endOfDay);

        // SQL 파라미터 설정
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchfrdate", fromDate);
        dicParam.addValue("searchtodate", toDate);
        dicParam.addValue("sitename", "%" +searchsitename+ "%");
        dicParam.addValue("esname", "%" + searchesname + "%");


        // SQL 쿼리
        String sql = """
        select *, FORMAT(servicertm, 'YYYY-MM-DD"T"HH24:MI') AS checkstdt,
        FORMAT(serviceftm, 'YYYY-MM-DD"T"HH24:MI') AS checkendt
        from tb_rp810
        where indatem between :searchfrdate and :searchtodate
        and sitename like :sitename
        and esname like :esname
    """;

        return this.sqlRunner.getRows(sql, dicParam);
    }

    public List<Map<String, Object>> getFileList2(String SpunCode){

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("spuncode", SpunCode);

        String sql = """
                    select fileornm, filesize, fileextns, spuncode_id, checkseq, filepath, filesvnm
                    from tb_rp815
                    where "spuncode_id" = :spuncode
                    order by checkseq
                """;
        return this.sqlRunner.getRows(sql, dicParam);
    }

    public Boolean update(TB_RP810Dto dto, MultipartFile[] files) throws IOException {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        try{
            String sql  = """
                    update tb_rp810 set srnumber = :srnumber, purpvisit = :purpvisit, sitename = :sitename,
                    esname = :esname, sncode = :sncode, servicecause = :servicecause, addregitem = :addregitem,
                    servicertm = :servicertm, serviceftm = :serviceftm, fsresponnm = :fsresponnm, fsresponid = fsresponid
                    where spuncode = :spuncode
                """;
            dicParam.addValue("srnumber", dto.getSrnumber());
            dicParam.addValue("purpvisit", dto.getPurpvisit());
            dicParam.addValue("sitename", dto.getSitename());
            dicParam.addValue("esname", dto.getEsname());
            dicParam.addValue("sncode", dto.getSncode());
            dicParam.addValue("servicecause", dto.getServicecause());
            dicParam.addValue("addregitem", dto.getAddregitem());
            dicParam.addValue("servicertm", dto.getServicertm());
            dicParam.addValue("serviceftm", dto.getServiceftm());
            dicParam.addValue("fsresponnm", dto.getFsresponnm());
            dicParam.addValue("fsresponid", dto.getFsresponid());
            dicParam.addValue("spuncode", dto.getSpuncode());



            Map<String, Object> metaItem = new HashMap<>();

            metaItem.put("spworkcd", dto.getSpworkcd());
            metaItem.put("spworknm", dto.getSpworknm());
            metaItem.put("spcompcd", dto.getSpcompcd());
            metaItem.put("spcompnm", dto.getSpcompnm());
            metaItem.put("spplancd", dto.getSpplancd());
            metaItem.put("spplannm", dto.getSpplannm());
            metaItem.put("spuncode", dto.getSpuncode());
            metaItem.put("inuserid", dto.getInuserid());
            metaItem.put("inusernm", dto.getInusernm());

            String path = settings.getProperty("file_upload_path") + "FieldService첨부파일";


            if(files != null){

                List<TB_RP815> entityList = new ArrayList<>();

                Optional<String> checkseqvalue = tbRp815Repository.findMaxChecknoByCheckdt(dto.getSpuncode());

                int checknointValue;

                checknointValue = checkseqvalue.map(Integer::parseInt).orElse(0);

                for (MultipartFile filelist: files){

                    ++checknointValue;

                    Map<String, Object> fileinform =  fileService.saveFiles(filelist, path); //DISK 저장

                    entityList.add(TB_RP815_Save(metaItem, fileinform, "N", checknointValue));  //DB 저장
                }
                tbRp815Repository.saveAll(entityList);
            }
            this.sqlRunner.execute(sql, dicParam);
        }catch(Exception e){
            e.getMessage();
            return false;
        }
        return true;
    }
}
