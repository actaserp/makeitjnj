package mes.app.request.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_DA006WFile;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RequestService {

  @Autowired
  SqlRunner sqlRunner;

  @Autowired
  TB_DA006WFILERepository tbDa006WFILERepository;

  //BOM 리스트
  public List<Map<String, Object>> getBomList(Integer materialId) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("Material_id", materialId);
    String sql = """
        select b.id from bom b 
        left join material m on m.id = b.Material_id
        where m.id = :Material_id
        """;
    return sqlRunner.getRows(sql, param);
  }

  public List<Map<String, Object>> getHoliday(){
    MapSqlParameterSource dicParam = new MapSqlParameterSource();

    String sql = """
               select * from TB_PZ010
                """;

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
    return items;
  }

  public boolean saveFile(TB_DA006WFile tbDa006WFile) {
    try {
      tbDa006WFILERepository.save(tbDa006WFile);
      return true;

    } catch (Exception e) {
      System.out.println(e + ": 에러발생");
      return false;
    }
  }
}
