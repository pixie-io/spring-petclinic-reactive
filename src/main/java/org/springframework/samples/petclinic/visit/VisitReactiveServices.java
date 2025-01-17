package org.springframework.samples.petclinic.visit;

import java.time.LocalDate;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.samples.petclinic.pet.db.PetEntity;
import org.springframework.samples.petclinic.pet.db.PetReactiveDao;
import org.springframework.samples.petclinic.utils.MappingUtils;
import org.springframework.samples.petclinic.vet.db.VetEntity;
import org.springframework.samples.petclinic.visit.db.VisitEntity;
import org.springframework.samples.petclinic.visit.db.VisitReactiveDao;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of services for Visit
 * 
 * This class leverages on multiple Dao (visit, pet, reference lists) 
 * to compose data needed at UI level.
 */
@Component
@Validated
public class VisitReactiveServices implements InitializingBean {

    public static final String VISIT_SPECIALTY = "vet_specialty";

    /** Implementation of Crud operations for pets. */
    private PetReactiveDao petDao;
    
    /** Implementation of Crud operations for visits. */
    private VisitReactiveDao visitDao;
    
    /**
     * Visit services.
     */
    public VisitReactiveServices(PetReactiveDao petDao, VisitReactiveDao visitDao) {
        this.petDao = petDao;
        this.visitDao = visitDao;
    }
    
    /**
     * Retrieve all visits from the database.
     * 
     * @return
     *      a Flux (reactor) with the stream of Owners.
     */
    public Flux<Visit> findAllVisits() {
        return Flux.from(visitDao.findAll())
                   .map(MappingUtils::mapEntityAsVisit)
                   .flatMap(petDao::populatePetForVisit);
    }
    
    /**
     * Retrieve a visit from its identifier in DB.
     * 
     * @return
     *      a Mono of Visit or empty
     */
    public Mono<Visit> findVisitById(@NotBlank String visitId) {
        return Mono.from(visitDao.findVisitById((UUID.fromString(visitId))))
                   .map(MappingUtils::mapEntityAsVisit)
                   .flatMap(petDao::populatePetForVisit);
    }
    
    /**
     * Create a visit in table petclinic_visit_by_pet
     * 
     * @param v
     *      curren Visit Bean
     * @return
     *      empty mono
     */
    public Mono<Visit> createVisit(@NotNull Visit v) {
        VisitEntity ve = MappingUtils.mapVisitAsEntity(v);
        return Mono.from(visitDao.upsert(ve))
                   .map(rr -> ve)
                   .map(MappingUtils::mapEntityAsVisit);
    }
    
    /**
     * Delete a visit from its id if it exits.
     *
     * @param visitId
     *      unique visit identifier
     * @return
     *      emmpty mono
     */
    public Mono<Void> deleteVisitById(UUID visitId) {
        return Mono.from(visitDao.findVisitById(visitId))
                   .map(visitDao::delete)
                   .then();
    }

    /**
     * Spring interface {@link InitializingBean} let you execute some
     * code after bean has been initialized.
     *
     * Here we enter default values for Pets and Pet visits.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        UUID petUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Mono.from(petDao.upsert(new PetEntity(UUID.randomUUID(), petUuid,
                "Dog", "Ollie", LocalDate.of(2021, 5, 10)))).subscribe();
        for (int i = 0; i < 10; i++)
            Mono.from(visitDao.upsert(new VisitEntity(petUuid, UUID.randomUUID(),
                    "General check up for Ollie", LocalDate.of(2023, 1, 25)))).subscribe();
    }
}
