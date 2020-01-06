package com.diviso.graeshoppe.customer.web.rest;

import com.diviso.graeshoppe.customer.CustomerApp;
import com.diviso.graeshoppe.customer.config.TestSecurityConfiguration;
import com.diviso.graeshoppe.customer.domain.UniqueCustomerID;
import com.diviso.graeshoppe.customer.repository.UniqueCustomerIDRepository;
import com.diviso.graeshoppe.customer.repository.search.UniqueCustomerIDSearchRepository;
import com.diviso.graeshoppe.customer.service.UniqueCustomerIDService;
import com.diviso.graeshoppe.customer.service.dto.UniqueCustomerIDDTO;
import com.diviso.graeshoppe.customer.service.mapper.UniqueCustomerIDMapper;
import com.diviso.graeshoppe.customer.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;

import static com.diviso.graeshoppe.customer.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link UniqueCustomerIDResource} REST controller.
 */
@SpringBootTest(classes = {CustomerApp.class, TestSecurityConfiguration.class})
public class UniqueCustomerIDResourceIT {

    @Autowired
    private UniqueCustomerIDRepository uniqueCustomerIDRepository;

    @Autowired
    private UniqueCustomerIDMapper uniqueCustomerIDMapper;

    @Autowired
    private UniqueCustomerIDService uniqueCustomerIDService;

    /**
     * This repository is mocked in the com.diviso.graeshoppe.customer.repository.search test package.
     *
     * @see com.diviso.graeshoppe.customer.repository.search.UniqueCustomerIDSearchRepositoryMockConfiguration
     */
    @Autowired
    private UniqueCustomerIDSearchRepository mockUniqueCustomerIDSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restUniqueCustomerIDMockMvc;

    private UniqueCustomerID uniqueCustomerID;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final UniqueCustomerIDResource uniqueCustomerIDResource = new UniqueCustomerIDResource(uniqueCustomerIDService);
        this.restUniqueCustomerIDMockMvc = MockMvcBuilders.standaloneSetup(uniqueCustomerIDResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UniqueCustomerID createEntity(EntityManager em) {
        UniqueCustomerID uniqueCustomerID = new UniqueCustomerID();
        return uniqueCustomerID;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UniqueCustomerID createUpdatedEntity(EntityManager em) {
        UniqueCustomerID uniqueCustomerID = new UniqueCustomerID();
        return uniqueCustomerID;
    }

    @BeforeEach
    public void initTest() {
        uniqueCustomerID = createEntity(em);
    }

    @Test
    @Transactional
    public void createUniqueCustomerID() throws Exception {
        int databaseSizeBeforeCreate = uniqueCustomerIDRepository.findAll().size();

        // Create the UniqueCustomerID
        UniqueCustomerIDDTO uniqueCustomerIDDTO = uniqueCustomerIDMapper.toDto(uniqueCustomerID);
        restUniqueCustomerIDMockMvc.perform(post("/api/unique-customer-ids")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(uniqueCustomerIDDTO)))
            .andExpect(status().isCreated());

        // Validate the UniqueCustomerID in the database
        List<UniqueCustomerID> uniqueCustomerIDList = uniqueCustomerIDRepository.findAll();
        assertThat(uniqueCustomerIDList).hasSize(databaseSizeBeforeCreate + 1);
        UniqueCustomerID testUniqueCustomerID = uniqueCustomerIDList.get(uniqueCustomerIDList.size() - 1);

        // Validate the UniqueCustomerID in Elasticsearch
        verify(mockUniqueCustomerIDSearchRepository, times(1)).save(testUniqueCustomerID);
    }

    @Test
    @Transactional
    public void createUniqueCustomerIDWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = uniqueCustomerIDRepository.findAll().size();

        // Create the UniqueCustomerID with an existing ID
        uniqueCustomerID.setId(1L);
        UniqueCustomerIDDTO uniqueCustomerIDDTO = uniqueCustomerIDMapper.toDto(uniqueCustomerID);

        // An entity with an existing ID cannot be created, so this API call must fail
        restUniqueCustomerIDMockMvc.perform(post("/api/unique-customer-ids")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(uniqueCustomerIDDTO)))
            .andExpect(status().isBadRequest());

        // Validate the UniqueCustomerID in the database
        List<UniqueCustomerID> uniqueCustomerIDList = uniqueCustomerIDRepository.findAll();
        assertThat(uniqueCustomerIDList).hasSize(databaseSizeBeforeCreate);

        // Validate the UniqueCustomerID in Elasticsearch
        verify(mockUniqueCustomerIDSearchRepository, times(0)).save(uniqueCustomerID);
    }


    @Test
    @Transactional
    public void getAllUniqueCustomerIDS() throws Exception {
        // Initialize the database
        uniqueCustomerIDRepository.saveAndFlush(uniqueCustomerID);

        // Get all the uniqueCustomerIDList
        restUniqueCustomerIDMockMvc.perform(get("/api/unique-customer-ids?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(uniqueCustomerID.getId().intValue())));
    }
    
    @Test
    @Transactional
    public void getUniqueCustomerID() throws Exception {
        // Initialize the database
        uniqueCustomerIDRepository.saveAndFlush(uniqueCustomerID);

        // Get the uniqueCustomerID
        restUniqueCustomerIDMockMvc.perform(get("/api/unique-customer-ids/{id}", uniqueCustomerID.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(uniqueCustomerID.getId().intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingUniqueCustomerID() throws Exception {
        // Get the uniqueCustomerID
        restUniqueCustomerIDMockMvc.perform(get("/api/unique-customer-ids/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateUniqueCustomerID() throws Exception {
        // Initialize the database
        uniqueCustomerIDRepository.saveAndFlush(uniqueCustomerID);

        int databaseSizeBeforeUpdate = uniqueCustomerIDRepository.findAll().size();

        // Update the uniqueCustomerID
        UniqueCustomerID updatedUniqueCustomerID = uniqueCustomerIDRepository.findById(uniqueCustomerID.getId()).get();
        // Disconnect from session so that the updates on updatedUniqueCustomerID are not directly saved in db
        em.detach(updatedUniqueCustomerID);
        UniqueCustomerIDDTO uniqueCustomerIDDTO = uniqueCustomerIDMapper.toDto(updatedUniqueCustomerID);

        restUniqueCustomerIDMockMvc.perform(put("/api/unique-customer-ids")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(uniqueCustomerIDDTO)))
            .andExpect(status().isOk());

        // Validate the UniqueCustomerID in the database
        List<UniqueCustomerID> uniqueCustomerIDList = uniqueCustomerIDRepository.findAll();
        assertThat(uniqueCustomerIDList).hasSize(databaseSizeBeforeUpdate);
        UniqueCustomerID testUniqueCustomerID = uniqueCustomerIDList.get(uniqueCustomerIDList.size() - 1);

        // Validate the UniqueCustomerID in Elasticsearch
        verify(mockUniqueCustomerIDSearchRepository, times(1)).save(testUniqueCustomerID);
    }

    @Test
    @Transactional
    public void updateNonExistingUniqueCustomerID() throws Exception {
        int databaseSizeBeforeUpdate = uniqueCustomerIDRepository.findAll().size();

        // Create the UniqueCustomerID
        UniqueCustomerIDDTO uniqueCustomerIDDTO = uniqueCustomerIDMapper.toDto(uniqueCustomerID);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUniqueCustomerIDMockMvc.perform(put("/api/unique-customer-ids")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(uniqueCustomerIDDTO)))
            .andExpect(status().isBadRequest());

        // Validate the UniqueCustomerID in the database
        List<UniqueCustomerID> uniqueCustomerIDList = uniqueCustomerIDRepository.findAll();
        assertThat(uniqueCustomerIDList).hasSize(databaseSizeBeforeUpdate);

        // Validate the UniqueCustomerID in Elasticsearch
        verify(mockUniqueCustomerIDSearchRepository, times(0)).save(uniqueCustomerID);
    }

    @Test
    @Transactional
    public void deleteUniqueCustomerID() throws Exception {
        // Initialize the database
        uniqueCustomerIDRepository.saveAndFlush(uniqueCustomerID);

        int databaseSizeBeforeDelete = uniqueCustomerIDRepository.findAll().size();

        // Delete the uniqueCustomerID
        restUniqueCustomerIDMockMvc.perform(delete("/api/unique-customer-ids/{id}", uniqueCustomerID.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<UniqueCustomerID> uniqueCustomerIDList = uniqueCustomerIDRepository.findAll();
        assertThat(uniqueCustomerIDList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the UniqueCustomerID in Elasticsearch
        verify(mockUniqueCustomerIDSearchRepository, times(1)).deleteById(uniqueCustomerID.getId());
    }

    @Test
    @Transactional
    public void searchUniqueCustomerID() throws Exception {
        // Initialize the database
        uniqueCustomerIDRepository.saveAndFlush(uniqueCustomerID);
        when(mockUniqueCustomerIDSearchRepository.search(queryStringQuery("id:" + uniqueCustomerID.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(uniqueCustomerID), PageRequest.of(0, 1), 1));
        // Search the uniqueCustomerID
        restUniqueCustomerIDMockMvc.perform(get("/api/_search/unique-customer-ids?query=id:" + uniqueCustomerID.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(uniqueCustomerID.getId().intValue())));
    }
}
