package com.diviso.graeshoppe.customer.web.rest;

import com.diviso.graeshoppe.customer.CustomerApp;
import com.diviso.graeshoppe.customer.config.TestSecurityConfiguration;
import com.diviso.graeshoppe.customer.domain.FavouriteProduct;
import com.diviso.graeshoppe.customer.repository.FavouriteProductRepository;
import com.diviso.graeshoppe.customer.repository.search.FavouriteProductSearchRepository;
import com.diviso.graeshoppe.customer.service.FavouriteProductService;
import com.diviso.graeshoppe.customer.service.dto.FavouriteProductDTO;
import com.diviso.graeshoppe.customer.service.mapper.FavouriteProductMapper;
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
 * Integration tests for the {@link FavouriteProductResource} REST controller.
 */
@SpringBootTest(classes = {CustomerApp.class, TestSecurityConfiguration.class})
public class FavouriteProductResourceIT {

    private static final Long DEFAULT_PRODUCT_ID = 1L;
    private static final Long UPDATED_PRODUCT_ID = 2L;

    @Autowired
    private FavouriteProductRepository favouriteProductRepository;

    @Autowired
    private FavouriteProductMapper favouriteProductMapper;

    @Autowired
    private FavouriteProductService favouriteProductService;

    /**
     * This repository is mocked in the com.diviso.graeshoppe.customer.repository.search test package.
     *
     * @see com.diviso.graeshoppe.customer.repository.search.FavouriteProductSearchRepositoryMockConfiguration
     */
    @Autowired
    private FavouriteProductSearchRepository mockFavouriteProductSearchRepository;

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

    private MockMvc restFavouriteProductMockMvc;

    private FavouriteProduct favouriteProduct;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final FavouriteProductResource favouriteProductResource = new FavouriteProductResource(favouriteProductService);
        this.restFavouriteProductMockMvc = MockMvcBuilders.standaloneSetup(favouriteProductResource)
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
    public static FavouriteProduct createEntity(EntityManager em) {
        FavouriteProduct favouriteProduct = new FavouriteProduct()
            .productId(DEFAULT_PRODUCT_ID);
        return favouriteProduct;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FavouriteProduct createUpdatedEntity(EntityManager em) {
        FavouriteProduct favouriteProduct = new FavouriteProduct()
            .productId(UPDATED_PRODUCT_ID);
        return favouriteProduct;
    }

    @BeforeEach
    public void initTest() {
        favouriteProduct = createEntity(em);
    }

    @Test
    @Transactional
    public void createFavouriteProduct() throws Exception {
        int databaseSizeBeforeCreate = favouriteProductRepository.findAll().size();

        // Create the FavouriteProduct
        FavouriteProductDTO favouriteProductDTO = favouriteProductMapper.toDto(favouriteProduct);
        restFavouriteProductMockMvc.perform(post("/api/favourite-products")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(favouriteProductDTO)))
            .andExpect(status().isCreated());

        // Validate the FavouriteProduct in the database
        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeCreate + 1);
        FavouriteProduct testFavouriteProduct = favouriteProductList.get(favouriteProductList.size() - 1);
        assertThat(testFavouriteProduct.getProductId()).isEqualTo(DEFAULT_PRODUCT_ID);

        // Validate the FavouriteProduct in Elasticsearch
        verify(mockFavouriteProductSearchRepository, times(1)).save(testFavouriteProduct);
    }

    @Test
    @Transactional
    public void createFavouriteProductWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = favouriteProductRepository.findAll().size();

        // Create the FavouriteProduct with an existing ID
        favouriteProduct.setId(1L);
        FavouriteProductDTO favouriteProductDTO = favouriteProductMapper.toDto(favouriteProduct);

        // An entity with an existing ID cannot be created, so this API call must fail
        restFavouriteProductMockMvc.perform(post("/api/favourite-products")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(favouriteProductDTO)))
            .andExpect(status().isBadRequest());

        // Validate the FavouriteProduct in the database
        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeCreate);

        // Validate the FavouriteProduct in Elasticsearch
        verify(mockFavouriteProductSearchRepository, times(0)).save(favouriteProduct);
    }


    @Test
    @Transactional
    public void checkProductIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = favouriteProductRepository.findAll().size();
        // set the field null
        favouriteProduct.setProductId(null);

        // Create the FavouriteProduct, which fails.
        FavouriteProductDTO favouriteProductDTO = favouriteProductMapper.toDto(favouriteProduct);

        restFavouriteProductMockMvc.perform(post("/api/favourite-products")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(favouriteProductDTO)))
            .andExpect(status().isBadRequest());

        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllFavouriteProducts() throws Exception {
        // Initialize the database
        favouriteProductRepository.saveAndFlush(favouriteProduct);

        // Get all the favouriteProductList
        restFavouriteProductMockMvc.perform(get("/api/favourite-products?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(favouriteProduct.getId().intValue())))
            .andExpect(jsonPath("$.[*].productId").value(hasItem(DEFAULT_PRODUCT_ID.intValue())));
    }
    
    @Test
    @Transactional
    public void getFavouriteProduct() throws Exception {
        // Initialize the database
        favouriteProductRepository.saveAndFlush(favouriteProduct);

        // Get the favouriteProduct
        restFavouriteProductMockMvc.perform(get("/api/favourite-products/{id}", favouriteProduct.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(favouriteProduct.getId().intValue()))
            .andExpect(jsonPath("$.productId").value(DEFAULT_PRODUCT_ID.intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingFavouriteProduct() throws Exception {
        // Get the favouriteProduct
        restFavouriteProductMockMvc.perform(get("/api/favourite-products/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateFavouriteProduct() throws Exception {
        // Initialize the database
        favouriteProductRepository.saveAndFlush(favouriteProduct);

        int databaseSizeBeforeUpdate = favouriteProductRepository.findAll().size();

        // Update the favouriteProduct
        FavouriteProduct updatedFavouriteProduct = favouriteProductRepository.findById(favouriteProduct.getId()).get();
        // Disconnect from session so that the updates on updatedFavouriteProduct are not directly saved in db
        em.detach(updatedFavouriteProduct);
        updatedFavouriteProduct
            .productId(UPDATED_PRODUCT_ID);
        FavouriteProductDTO favouriteProductDTO = favouriteProductMapper.toDto(updatedFavouriteProduct);

        restFavouriteProductMockMvc.perform(put("/api/favourite-products")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(favouriteProductDTO)))
            .andExpect(status().isOk());

        // Validate the FavouriteProduct in the database
        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeUpdate);
        FavouriteProduct testFavouriteProduct = favouriteProductList.get(favouriteProductList.size() - 1);
        assertThat(testFavouriteProduct.getProductId()).isEqualTo(UPDATED_PRODUCT_ID);

        // Validate the FavouriteProduct in Elasticsearch
        verify(mockFavouriteProductSearchRepository, times(1)).save(testFavouriteProduct);
    }

    @Test
    @Transactional
    public void updateNonExistingFavouriteProduct() throws Exception {
        int databaseSizeBeforeUpdate = favouriteProductRepository.findAll().size();

        // Create the FavouriteProduct
        FavouriteProductDTO favouriteProductDTO = favouriteProductMapper.toDto(favouriteProduct);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restFavouriteProductMockMvc.perform(put("/api/favourite-products")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(favouriteProductDTO)))
            .andExpect(status().isBadRequest());

        // Validate the FavouriteProduct in the database
        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeUpdate);

        // Validate the FavouriteProduct in Elasticsearch
        verify(mockFavouriteProductSearchRepository, times(0)).save(favouriteProduct);
    }

    @Test
    @Transactional
    public void deleteFavouriteProduct() throws Exception {
        // Initialize the database
        favouriteProductRepository.saveAndFlush(favouriteProduct);

        int databaseSizeBeforeDelete = favouriteProductRepository.findAll().size();

        // Delete the favouriteProduct
        restFavouriteProductMockMvc.perform(delete("/api/favourite-products/{id}", favouriteProduct.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<FavouriteProduct> favouriteProductList = favouriteProductRepository.findAll();
        assertThat(favouriteProductList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the FavouriteProduct in Elasticsearch
        verify(mockFavouriteProductSearchRepository, times(1)).deleteById(favouriteProduct.getId());
    }

    @Test
    @Transactional
    public void searchFavouriteProduct() throws Exception {
        // Initialize the database
        favouriteProductRepository.saveAndFlush(favouriteProduct);
        when(mockFavouriteProductSearchRepository.search(queryStringQuery("id:" + favouriteProduct.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(favouriteProduct), PageRequest.of(0, 1), 1));
        // Search the favouriteProduct
        restFavouriteProductMockMvc.perform(get("/api/_search/favourite-products?query=id:" + favouriteProduct.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(favouriteProduct.getId().intValue())))
            .andExpect(jsonPath("$.[*].productId").value(hasItem(DEFAULT_PRODUCT_ID.intValue())));
    }
}
