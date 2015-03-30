/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.account.subscription.factory;

import com.codenvy.api.account.subscription.SubscriptionEvent;
import com.codenvy.api.account.subscription.service.util.SubscriptionCharger;
import com.codenvy.api.account.subscription.service.util.SubscriptionServiceHelper;
import com.codenvy.api.account.subscription.service.util.SubscriptionTrialRemover;

import org.eclipse.che.api.account.server.SubscriptionService;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Subscription;
import org.eclipse.che.api.account.shared.dto.UsedAccountResources;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.codenvy.api.account.subscription.ServiceId.FACTORY;

/**
 * Subscription of factories
 *
 * @author Sergii Kabashniuk
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@Singleton
public class FactorySubscriptionService extends SubscriptionService {
    private static final Logger LOG = LoggerFactory.getLogger(FactorySubscriptionService.class);

    private final AccountDao                accountDao;
    private final SubscriptionCharger       chargeUtil;
    private final SubscriptionTrialRemover  removeUtil;
    private final EventService              eventService;
    private final SubscriptionServiceHelper subscriptionServiceHelper;

    @Inject
    public FactorySubscriptionService(AccountDao accountDao,
                                      SubscriptionCharger chargeUtil,
                                      SubscriptionTrialRemover removeUtil,
                                      EventService eventService,
                                      SubscriptionServiceHelper subscriptionServiceHelper) {
        super(FACTORY, FACTORY);
        this.accountDao = accountDao;
        this.chargeUtil = chargeUtil;
        this.removeUtil = removeUtil;
        this.eventService = eventService;
        this.subscriptionServiceHelper = subscriptionServiceHelper;
    }

    @Override
    public void beforeCreateSubscription(Subscription subscription) throws ConflictException, ServerException, ForbiddenException {
        String tariffPackage;
        if (null == (tariffPackage = subscription.getProperties().get("Package"))) {
            throw new ConflictException("Subscription property 'Package' required");
        } else if (!"tracked".equals(tariffPackage.toLowerCase())) {
            throw new ConflictException(String.format("Package '%s' is unknown", tariffPackage));
        }
        if (subscription.getProperties().get("RAM") == null) {
            throw new ConflictException("Subscription property 'RAM' required");
        }
        try {
            if (accountDao.getActiveSubscription(subscription.getAccountId(), getServiceId()) != null) {
                throw new ConflictException(SUBSCRIPTION_LIMIT_EXHAUSTED_MESSAGE);
            }
        } catch (NotFoundException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        }
        subscriptionServiceHelper.checkCreditCard(subscription);
        subscriptionServiceHelper.setDates(subscription);
    }

    @Override
    public void afterCreateSubscription(Subscription subscription) throws ApiException {
        subscriptionServiceHelper.chargeSubscriptionIfNeed(subscription);
        eventService.publish(SubscriptionEvent.subscriptionAddedEvent(subscription));
    }

    @Override
    public void onRemoveSubscription(Subscription subscription) throws ApiException {
        eventService.publish(SubscriptionEvent.subscriptionRemovedEvent(subscription));
    }

    @Override
    public void onUpdateSubscription(Subscription oldSubscription, Subscription newSubscription) throws ApiException {

    }

    @Override
    public void onCheckSubscriptions() throws ApiException {
        removeUtil.removeExpiredTrial(this);
        chargeUtil.charge(this);

//        TODO It is need to send emails about trial expiration?
//        expirationUtil.sendEmailAboutExpiringTrial(getServiceId(), 2);
//        expirationUtil.sendEmailAboutExpiredTrial(getServiceId(), 2);
//        expirationUtil.sendEmailAboutExpiredTrial(getServiceId(), 7);
    }

    @Override
    public UsedAccountResources getAccountResources(Subscription subscription) throws ServerException {
        return DtoFactory.getInstance().createDto(UsedAccountResources.class);
    }
}