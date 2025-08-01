package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.ModelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModelHistoryRepository extends JpaRepository<ModelHistory, Integer> {

  Optional<Integer> findMaxVersionNoByModelid(String modelid);

  @Modifying
  @Query("""
  DELETE FROM ModelHistory m 
  WHERE m.modelid = :modelid 
    AND m.custcd = :custcd 
    AND m.spjangcd = :spjangcd 
    AND m.reqdate = :reqdate 
    AND m.reqnum = :reqnum
""")  void deleteByModelHistoryKey( @Param("modelid") String modelid, @Param("custcd") String custcd,
                                    @Param("spjangcd") String spjangcd, @Param("reqdate") String reqdate,
                                    @Param("reqnum") String reqnum);

}
