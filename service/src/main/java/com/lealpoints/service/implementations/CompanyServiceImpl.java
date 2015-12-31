package com.lealpoints.service.implementations;

import com.lealpoints.context.ThreadContextService;
import com.lealpoints.i18n.Message;
import com.lealpoints.model.*;
import com.lealpoints.repository.*;
import com.lealpoints.service.CompanyService;
import com.lealpoints.service.annotation.OnlyProduction;
import com.lealpoints.service.model.CompanyRegistration;
import com.lealpoints.service.model.ValidationResult;
import com.lealpoints.service.response.ServiceMessage;
import com.lealpoints.service.response.ServiceResult;
import com.lealpoints.util.EmailUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Component
public class CompanyServiceImpl extends BaseServiceImpl implements CompanyService {
    private static final Logger logger = LogManager.getLogger(CompanyServiceImpl.class.getName());
    private final CompanyRepository _companyRepository;
    private final CompanyUserRepository _companyUserRepository;
    private final PointsConfigurationRepository _pointsConfigurationRepository;
    private final ClientRepository _clientRepository;
    private final SMSServiceImpl _smsService;
    private final CompanyClientMappingRepository _companyClientMappingRepository;
    private final PromotionConfigurationRepository _promotionConfigurationRepository;
    private final ConfigurationServiceImpl _configurationManager;

    @Autowired
    public CompanyServiceImpl(CompanyRepository companyRepository, CompanyUserRepository companyUserRepository,
                              PointsConfigurationRepository pointsConfigurationRepository, ClientRepository clientRepository, ThreadContextService threadContextService,
                              SMSServiceImpl smsService, CompanyClientMappingRepository companyClientMappingRepository,
                              PromotionConfigurationRepository promotionConfigurationRepository, ConfigurationServiceImpl configurationManager) {
        super(threadContextService);
        _companyRepository = companyRepository;
        _companyUserRepository = companyUserRepository;
        _pointsConfigurationRepository = pointsConfigurationRepository;
        _clientRepository = clientRepository;
        _smsService = smsService;
        _companyClientMappingRepository = companyClientMappingRepository;
        _promotionConfigurationRepository = promotionConfigurationRepository;
        _configurationManager = configurationManager;
    }

    public ServiceResult<String> register(CompanyRegistration companyRegistration) {
        try {
            ValidationResult validationResult = validateRegistration(companyRegistration);
            if (validationResult.isValid()) {
                getThreadContextService().getQueryAgent().beginTransaction();
                final String activationKey = RandomStringUtils.random(60, true, true);
                long companyId = registerAndInitializeCompany(companyRegistration, activationKey);
                getQueryAgent().commitTransaction();
                return createServiceResult(companyId, activationKey);
            } else {
                return new ServiceResult<>(false, validationResult.getServiceMessage());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    private ServiceResult<String> createServiceResult(long companyId, String activationKey) {
        final ServiceMessage serviceMessage = getServiceMessage(Message.WE_HAVE_SENT_YOU_AND_ACTIVATION_LINK);
        final ServiceResult<String> serviceResult = new ServiceResult<>(true, serviceMessage, Long.toString(companyId));
        if (isFunctionalTestEnvironment()) {
            serviceResult.setExtraInfo(activationKey);
        }
        return serviceResult;
    }

    public ServiceResult<List<PointsInCompany>> getPointsInCompanyByPhone(String phone) {
        try {
            Client client = _clientRepository.getByPhone(phone);
            List<PointsInCompany> companies = _companyRepository.getPointsInCompanyByClientId(client.getClientId());
            return new ServiceResult<>(true, ServiceMessage.EMPTY, companies);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    public ServiceResult<Company> getByCompanyId(long companyId) {
        try {
            final Company company = _companyRepository.getByCompanyId(companyId);
            if (company != null) {
                company.setUrlImageLogo(company.getUrlImageLogo() + "?" + new Date().getTime());
                return new ServiceResult<>(true, ServiceMessage.EMPTY, company);
            } else {
                logger.error("None company has the companyId: " + companyId);
                return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR));
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR), null);
        }
    }

    public ServiceResult<Boolean> updateLogo(List<FileItem> fileItems, long companyId) {
        try {
            if (fileItems.size() > 0) {
                final FileItem fileItem = fileItems.get(0);
                final String contentType = fileItem.getContentType();
                if (!isValidContentType(contentType)) {
                    return new ServiceResult<>(false, getServiceMessage(Message.INVALID_LOGO_FILE));
                }
                final String fileName = companyId + "." + getExtensionFromContentType(contentType);
                fileItem.write(new File(String.valueOf(getThreadContext().getEnvironment().getImageDir() + fileName)));
                _companyRepository.updateUrlImageLogo(companyId, fileName);
            } else {
                return new ServiceResult<>(false, getServiceMessage(Message.COULD_NOT_READ_FILE));
            }
            return new ServiceResult<>(true, getServiceMessage(Message.YOUR_LOGO_WAS_UPDATED));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR));
        }
    }

    @OnlyProduction
    public ServiceResult sendMobileAppAdMessage(long companyId, String phone) {
        final boolean sendPromoSmsInUat = Boolean.parseBoolean(_configurationManager.getUncachedConfiguration("send_promo_sms_in_uat"));
        if (isProdEnvironment() || sendPromoSmsInUat) {
            try {
                final Company company = _companyRepository.getByCompanyId(companyId);
                long clientId = _clientRepository.getByPhone(phone).getClientId();
                final double points = _companyClientMappingRepository.getByCompanyIdClientId(companyId, clientId).getPoints();
                if (company != null) {
                    final String smsMessage = getSMSMessage(company.getName(), points);
                    logger.info("Promo SMS sent to: " + phone);
                    _smsService.sendSMSMessage(phone, smsMessage);
                    _clientRepository.updateCanReceivePromoSms(clientId, false);
                } else {
                    logger.error("None company has the companyId: " + companyId);
                    return new ServiceResult<>(false, getServiceMessage(Message.COMMON_USER_ERROR));
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                return new ServiceResult(false, getServiceMessage(Message.MOBILE_APP_AD_MESSAGE_WAS_NOT_SENT_SUCCESSFULLY));
            }
            return new ServiceResult(true, getServiceMessage(Message.MOBILE_APP_AD_MESSAGE_SENT_SUCCESSFULLY));
        }
        return new ServiceResult(false, getServiceMessage(Message.COMMON_USER_ERROR));
    }

    public String getSMSMessage(String companyName, double points) {
        final String translation = getServiceMessage(Message.MOBILE_APP_AD_MESSAGE).getMessage();
        int SMS_MESSAGE_MAX_CHAR = 160;
        final String appUrl = "https://goo.gl/JRssA6";
        final int formattedMessageLength = String.format(translation, points, "", appUrl).length();
        if (formattedMessageLength >= SMS_MESSAGE_MAX_CHAR) {
            throw new IllegalArgumentException("Message length must be less than " + SMS_MESSAGE_MAX_CHAR + " in: " + translation);
        }
        final int maxCompanyNameLength = SMS_MESSAGE_MAX_CHAR - formattedMessageLength;
        String trimmedCompanyName = "";
        if (maxCompanyNameLength > 0) {
            trimmedCompanyName = StringUtils.abbreviate(companyName, Math.max(Math.min(maxCompanyNameLength, companyName.length()), 4));
        }
        return String.format(translation, new DecimalFormat("#.#").format(points), trimmedCompanyName, appUrl);
    }

    public File getLogo(long companyId) {
        try {
            final Company company = _companyRepository.getByCompanyId(companyId);
            if (company != null) {
                if (StringUtils.isEmpty(company.getUrlImageLogo())) {
                    return new File(getThreadContext().getEnvironment().getImageDir() + "no_image.png");
                } else {
                    return new File(getThreadContext().getEnvironment().getImageDir() + company.getUrlImageLogo());
                }
            } else {
                logger.error("None company has the companyId: " + companyId);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
        return null;
    }

    private boolean isValidContentType(String contentType) {
        return contentType.equalsIgnoreCase("image/jpeg") ||
                contentType.equalsIgnoreCase("image/png") ||
                contentType.equalsIgnoreCase("image/gif");
    }

    private String getExtensionFromContentType(String contentType) {
        String extension = "";
        if (contentType.equalsIgnoreCase("image/jpeg")) {
            extension = "jpeg";
        } else if (contentType.equalsIgnoreCase("image/png")) {
            extension = "png";
        } else if (contentType.equalsIgnoreCase("image/gif")) {
            extension = "gif";
        }
        return extension;
    }

    private long registerAndInitializeCompany(CompanyRegistration companyRegistration, String activationKey) throws Exception {
        long companyId = registerCompany(companyRegistration);
        registerPointsConfiguration(companyId);
        registerPromotionConfiguration(companyId);
        registerCompanyUser(companyRegistration, companyId, activationKey);
        sendActivationEmail(companyRegistration.getEmail(), activationKey);
        return companyId;
    }

    private String registerCompanyUser(CompanyRegistration companyRegistration, long companyId, String activationKey) throws Exception {
        CompanyUser companyUser = new CompanyUser();
        companyUser.setCompanyId(companyId);
        companyUser.setName(companyRegistration.getUserName());
        companyUser.setPassword(companyRegistration.getPassword());
        companyUser.setEmail(companyRegistration.getEmail());
        companyUser.setActive(false);
        companyUser.setActivationKey(activationKey);
        String language = companyRegistration.getLanguage();
        if (StringUtils.isNotEmpty(language)) {
            language = language.substring(0, 2);
        }
        companyUser.setLanguage(language);
        companyUser.setMustChangePassword(false);
        _companyUserRepository.insert(companyUser);
        return activationKey;
    }

    private void registerPromotionConfiguration(long companyId) throws Exception {
        PromotionConfiguration promotionConfiguration = new PromotionConfiguration();
        promotionConfiguration.setCompanyId(companyId);
        promotionConfiguration.setRequiredPoints(1000);
        promotionConfiguration.setDescription(getServiceMessage(Message.DEFAULT_PROMOTION_MESSAGE).getMessage());
        _promotionConfigurationRepository.insert(promotionConfiguration);
    }

    private void registerPointsConfiguration(long companyId) throws Exception {
        PointsConfiguration pointsConfiguration = new PointsConfiguration();
        pointsConfiguration.setCompanyId(companyId);
        pointsConfiguration.setPointsToEarn(1);
        pointsConfiguration.setRequiredAmount(1);
        _pointsConfigurationRepository.insert(pointsConfiguration);
    }

    private long registerCompany(CompanyRegistration companyRegistration) throws Exception {
        Company company = new Company();
        company.setName(companyRegistration.getCompanyName());
        company.setUrlImageLogo(companyRegistration.getUrlImageLogo());
        return _companyRepository.insert(company);
    }

    void sendActivationEmail(String email, String activationKey) throws MessagingException {
        if (isProdEnvironment() || isUATEnvironment()) {
            NotificationEmail notificationEmail = new NotificationEmail();
            notificationEmail.setSubject(getServiceMessage(Message.ACTIVATION_EMAIL_SUBJECT).getMessage());
            final String activationUrl = getActivationUrl(activationKey);
            notificationEmail.setBody(getServiceMessage(Message.ACTIVATION_EMAIL_BODY) + "\n\n" + activationUrl);
            notificationEmail.setEmailTo(email);
            EmailUtil.sendEmail(notificationEmail);
        }
    }

    private String getActivationUrl(String activationKey) {
        return getEnvironment().getClientUrl() + "activate?key=" + activationKey;
    }

    private ValidationResult validateRegistration(CompanyRegistration companyRegistration) throws Exception {
        //Validate company name
        if (StringUtils.isEmpty(companyRegistration.getCompanyName())) {
            return new ValidationResult(false, getServiceMessage(Message.COMPANY_NAME_IS_EMPTY));
        }
        //Validate user password
        if (companyRegistration.getPassword() == null || companyRegistration.getPassword().length() < 6) {
            return new ValidationResult(false, getServiceMessage(Message.PASSWORD_MUST_HAVE_AT_LEAST_6_CHARACTERS));
        }
        if (!companyRegistration.getPassword().equals(companyRegistration.getPasswordConfirmation())) {
            return new ValidationResult(false, getServiceMessage(Message.PASSWORD_AND_CONFIRMATION_ARE_DIFFERENT));
        }
        //Validate user email
        if (StringUtils.isEmpty(companyRegistration.getEmail())) {
            return new ValidationResult(false, getServiceMessage(Message.EMAIL_IS_EMPTY));
        }
        if (!EmailValidator.getInstance().isValid(companyRegistration.getEmail())) {
            return new ValidationResult(false, getServiceMessage(Message.EMAIL_IS_INVALID));
        }
        if (_companyUserRepository.getByEmail(companyRegistration.getEmail()) != null) {
            return new ValidationResult(false, getServiceMessage(Message.EMAIL_ALREADY_EXISTS));
        }
        return new ValidationResult(true, ServiceMessage.EMPTY);
    }
}