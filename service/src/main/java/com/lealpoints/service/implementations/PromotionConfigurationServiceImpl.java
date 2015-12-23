package com.lealpoints.service.implementations;

import com.lealpoints.context.ThreadContextService;
import com.lealpoints.i18n.Message;
import com.lealpoints.model.Client;
import com.lealpoints.model.CompanyClientMapping;
import com.lealpoints.model.PromotionConfiguration;
import com.lealpoints.repository.ClientRepository;
import com.lealpoints.repository.CompanyClientMappingRepository;
import com.lealpoints.repository.PromotionConfigurationRepository;
import com.lealpoints.service.PromotionConfigurationService;
import com.lealpoints.service.response.ServiceMessage;
import com.lealpoints.service.response.ServiceResult;
import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromotionConfigurationServiceImpl extends BaseServiceImpl implements PromotionConfigurationService {
    private static final Logger logger = LogManager.getLogger(PromotionConfigurationServiceImpl.class.getName());
    private final PromotionConfigurationRepository _promotionConfigurationRepository;
    private final CompanyClientMappingRepository _companyClientMappingRepository;
    private final ClientRepository _clientRepository;

    @Autowired
    public PromotionConfigurationServiceImpl(PromotionConfigurationRepository promotionConfigurationRepository,
                                             CompanyClientMappingRepository companyClientMappingRepository, ClientRepository clientRepository,
                                             ThreadContextService threadContextService) {
        super(threadContextService);
        _promotionConfigurationRepository = promotionConfigurationRepository;
        _companyClientMappingRepository = companyClientMappingRepository;
        _clientRepository = clientRepository;
    }

    public ServiceResult<List<PromotionConfiguration>> getByCompanyId(long companyId) {
        try {
            final List<PromotionConfiguration> promotionConfigurations = _promotionConfigurationRepository.getByCompanyId(companyId);
            return new ServiceResult<>(true, ServiceMessage.EMPTY, promotionConfigurations);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    public ServiceResult<Long> insert(PromotionConfiguration promotionConfiguration) {
        try {
            final long promotionConfigurationId = _promotionConfigurationRepository.insert(promotionConfiguration);
            return new ServiceResult<>(true, getServiceMessage(Message.PROMOTION_SUCCESSFULLY_ADDED), promotionConfigurationId);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    public ServiceResult<List<PromotionConfiguration>> getByCompanyIdRequiredPoints(long companyId, final String phone) {
        try {
            final Client client = _clientRepository.getByPhone(phone);
            if (client == null) {
                return new ServiceResult<>(false, getServiceMessage(Message.PHONE_NUMBER_DOES_NOT_EXIST));
            }
            final CompanyClientMapping companyClientMapping = _companyClientMappingRepository.getByCompanyIdClientId(companyId, client.getClientId());
            if (companyClientMapping == null) {
                return new ServiceResult<>(false, getServiceMessage(Message.PHONE_NUMBER_DOES_NOT_EXIST));
            }
            final List<PromotionConfiguration> promotionConfigurations = _promotionConfigurationRepository.getByCompanyId(companyId);
            final List<PromotionConfiguration> resultPromotionConfigurations =
                (List<PromotionConfiguration>) CollectionUtils.select(promotionConfigurations, new Predicate<PromotionConfiguration>() {
                    @Override
                    public boolean evaluate(PromotionConfiguration promotionConfiguration) {
                        return promotionConfiguration.getRequiredPoints() <= companyClientMapping.getPoints();
                    }
                });
            ServiceMessage message = ServiceMessage.EMPTY;
            if (resultPromotionConfigurations.isEmpty()) {
                message = getServiceMessage(Message.CLIENT_DOES_NOT_HAVE_AVAILABLE_PROMOTIONS);
            }
            return new ServiceResult<>(true, message, resultPromotionConfigurations);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    public ServiceResult<Boolean> deletePromotionConfiguration(long promotionConfigurationId) {
        try {
            final int deletedRows = _promotionConfigurationRepository.delete(promotionConfigurationId);
            if (deletedRows > 0) {
                ServiceMessage message = getServiceMessage(Message.THE_PROMOTION_WAS_DELETED);
                return new ServiceResult<>(true, message, true);
            } else {
                ServiceMessage message = getServiceMessage(Message.THE_PROMOTION_COULD_NOT_BE_DELETED);
                return new ServiceResult<>(false, message, false);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }
}