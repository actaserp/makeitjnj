package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TB_DA006WRepository extends JpaRepository<TB_DA006W, TB_DA006W_PK> {

    @Query("""
        SELECT COALESCE(MAX(CAST(t.id.reqnum AS int)), 1000) + 1
        FROM TB_DA006W t
        WHERE t.id.spjangcd = :spjangcd    
    """)
  String getNextReqnum(@Param("spjangcd") String spjangcd);

  @Modifying
  @Query("DELETE FROM TB_DA006W h WHERE h.id.custcd = :custcd AND h.id.spjangcd = :spjangcd AND h.id.reqdate = :reqdate AND h.id.reqnum = :reqnum")
  void deleteByPk(@Param("custcd") String custcd,
                  @Param("spjangcd") String spjangcd,
                  @Param("reqdate") String reqdate,
                  @Param("reqnum") String reqnum);

  @Query("SELECT t FROM TB_DA006W t WHERE t.id.reqnum = :reqnum")
  Optional<TB_DA006W> findByReqnum(@Param("reqnum") String reqnum);
}
