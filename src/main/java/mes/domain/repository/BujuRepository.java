package mes.domain.repository;

import mes.domain.entity.Balju;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BujuRepository extends JpaRepository<Balju, Integer> {
  Balju getBujuById(Integer id);
}
