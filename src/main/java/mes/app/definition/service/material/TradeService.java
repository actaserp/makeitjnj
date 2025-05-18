package mes.app.definition.service.material;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TradeService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> gettradeList(String name, String flag) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("name", name);
        dicParam.addValue("flag", flag);

        String sql = """
                    SELECT
                        A.trid
                        ,A.ioflag
                        ,A.tradenm
                        ,A.acccd
                        ,A.reacccd
                        ,A.remark
                    FROM tb_trade A
                     WHERE A.tradenm like concat('%',:name,'%')
                     AND A.ioflag like concat('%',:flag,'%')
                """;
        return this.sqlRunner.getRows(sql, dicParam);
    }


    public Map<String, Object> getTradeDetail(int idx) {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("idx", idx);

        String sql = """
                select trid
                ,ioflag
                ,tradenm
                ,acccd
                ,reacccd
                ,remark
                from tb_trade
                Where trid = :idx
			""";

        Map<String, Object> item = this.sqlRunner.getRow(sql, paramMap);

        return item;
    }


    public List<Map<String, Object>> getAccSearchitem(String code, String name) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("code", code);
        dicParam.addValue("name", name);

        String sql = """
                 select
                 A.acccd as acccd
                 , A.accnm as accnm
                 from tb_accsubject A
                where A.useyn = 'Y'
                AND A.spyn = '1'
                AND A.acccd like concat('%',:code,'%')
                AND A.accnm like concat('%',:name,'%')
                order by A.acccd
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }



}


