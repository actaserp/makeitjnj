package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006WFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TB_DA006WFILERepository extends JpaRepository<TB_DA006WFile,Integer> {

    @Query(value = "SELECT t.* FROM TB_DA006WFile t WHERE t.reqnum = :reqnum", nativeQuery = true)
    List<TB_DA006WFile> findAllByReqnum(String reqnum);

}
