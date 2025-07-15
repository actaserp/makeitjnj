package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface TB_DA006WRepository extends JpaRepository<TB_DA006W, TB_DA006W_PK> {
    @Query(value = "SELECT COALESCE(MAX(CAST(t.reqnum AS INT)), 0) FROM TB_DA006W t " +
            "WITH (UPDLOCK, ROWLOCK) " + // ✅ SQL Server에서는 FOR UPDATE 대신 사용
            "WHERE t.custcd = :custcd AND t.spjangcd = :spjangcd AND t.reqdate = :reqdate",
            nativeQuery = true)
    int findMaxReqnum(@Param("custcd") String custcd,
                      @Param("spjangcd") String spjangcd,
                      @Param("reqdate") String reqdate);

  @Query("""
        SELECT COALESCE(MAX(CAST(t.id.reqnum AS int)), 1000) + 1
        FROM TB_DA006W t
        WHERE t.id.custcd = :custcd
          AND t.id.spjangcd = :spjangcd
          AND t.id.reqdate = :reqdate
    """)
  String getNextReqnum(@Param("custcd") String custcd,
                       @Param("spjangcd") String spjangcd,
                       @Param("reqdate") String reqdate);
}
