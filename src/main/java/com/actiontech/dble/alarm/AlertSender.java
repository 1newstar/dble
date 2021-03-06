package com.actiontech.dble.alarm;

import com.actiontech.dble.DbleServer;
import com.actiontech.dble.cluster.ClusterController;
import com.actiontech.dble.cluster.ClusterGeneralConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Created by szf on 2019/3/22.
 */
public class AlertSender implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertSender.class);

    private final BlockingQueue<AlertTask> alertQueue;

    private static final Alert DEFAULT_ALERT = new NoAlert();
    private static volatile Alert alert;

    public AlertSender(BlockingQueue<AlertTask> alertQueue) {
        this.alertQueue = alertQueue;
        initAlert();
    }

    @Override
    public void run() {
        AlertTask alertTask;
        while (true) {
            try {
                alertTask = alertQueue.take();

                switch (alertTask.getAlertType()) {
                    case ALERT:
                        alert.alert(alertTask.getAlertBean());
                        break;
                    case ALERT_SELF:
                        alert.alertSelf(alertTask.getAlertBean());
                        break;
                    case ALERT_RESOLVE:
                        if (alert.alertResolve(alertTask.getAlertBean())) {
                            alertTask.alertCallBack();
                        }
                        break;
                    case ALERT_SELF_RESOLVE:
                        if (alert.alertSelfResolve(alertTask.getAlertBean())) {
                            alertTask.alertCallBack();
                        }
                        break;
                    default:
                        break;
                }
            } catch (Throwable e) {
                LOGGER.error("get error when send queue", e);
            }
        }
    }


    public void initAlert() {
        if (DbleServer.getInstance().isUseGeneralCluster() &&
                (ClusterController.CONFIG_MODE_UCORE.equals(ClusterGeneralConfig.getInstance().getClusterType()) ||
                        ClusterController.CONFIG_MODE_USHARD.equals(ClusterGeneralConfig.getInstance().getClusterType()))) {
            alert = new UcoreAlert();
        } else {
            alert = DEFAULT_ALERT;
        }
    }
}
