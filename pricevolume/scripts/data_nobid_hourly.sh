bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd -P`

install_root=`dirname ${bin}`
echo $install_root

# Use this for everything else

export PROJ_DIR=$install_root

export s3_rtb_dir=s3n://sharethis-logs-rtb
export s3_pc_dir=s3n://sharethis-logs-rtb-pc
export s3_nb_dir=s3n://sharethis-logs-rtb-nb
export BIN_DIR=${PROJ_DIR}/bin
export JAR_DIR=${PROJ_DIR}/jar
export CONF_DIR=${PROJ_DIR}/conf
export LOGS_DIR=${PROJ_DIR}/logs
export TMP_DIR=${PROJ_DIR}/tmp

if [ ! -d ${LOGS_DIR} ]; then
        mkdir -p ${LOGS_DIR}
fi
if [ ! -d ${TMP_DIR} ]; then
        mkdir -p ${TMP_DIR}
fi

. ${CONF_DIR}/env_pricevolume.sh

sendalert() {

message="$*"
echo $message
subject="[WARNING][Inventory Hourly] -- Hourly copying inventory data job failed while processing for hour: $hh on $pDate"
##maillist="insights@sharethis.com"
maillist=${email_address}

echo "The following step was failed while the job progressing" >> /tmp/mailbody_$$.txt
echo "Warning message :: $message " >> /tmp/mailbody_$$.txt

cat - /tmp/mailbody_$$.txt <<EOF | /usr/sbin/sendmail -t
To:$maillist
From:Watchdog<watchdog@sharethis.com>
Subject:${subject}

EOF

rm /tmp/mailbody_$$.txt
#exit 1
}

CONF_PROD="-conf ${CONF_DIR}/core-prod.xml"

if [ $1 == -1 ]
then
    let delay_hour_nb=2
    stringZ=`date -u +%Y%m%d%H -d "${delay_hour_nb} hour ago"`
else
    stringZ=$1
fi

echo "The date and time of the processing hour is ${stringZ}"  
yyyy=${stringZ:0:4}
mm=${stringZ:4:2}
dd=${stringZ:6:2}
hh=${stringZ:8:2}
v_date=${stringZ:0:8}
v_date_hour=${stringZ:0:10}
pDate="${yyyy}-${mm}-${dd}"

NOBID_BASE_DIR="${HDFS_DIR_RTB_NB}/rtb_nb"
NOBID_DATE_DIR="${NOBID_BASE_DIR}/${v_date}"
#NOBID_HOUR_DIR="${NOBID_DATE_DIR}/${hh}"
NOBID_HOUR_DIR="${NOBID_BASE_DIR}/${v_date_hour}"

s3_nb_date_dir=${s3_nb_dir}/${v_date}
s3_nb_hour_dir=${s3_nb_date_dir}/${hh}

qName="-Dmapred.job.queue.name=${QUEUENAME_INV_HOURLY}"

log_dir_nb=${LOG_TMP_DIR}/distcp_log_nb_${stringZ}
log="-log ${log_dir_nb}"

${HADOOP} fs -test -e ${NOBID_HOUR_DIR}/_SUCCESS
if [ $? -eq 0 ]
then
    echo "Found the _SUCCESS file in the partition folder :${NOBID_HOUR_DIR}. No copy is required!"
else
    echo "rm -rf ${log_dir_nb}" 
    rm -rf ${log_dir_nb} 

    echo "Before copying the nobid data from s3n, ${HADOOP} fs -rm -r ${NOBID_HOUR_DIR}" 
    ${HADOOP} fs -rm -r ${NOBID_HOUR_DIR} 

    echo "${HADOOP} distcp ${CONF_PROD} ${qName} -Ddistcp.bytes.per.map=102400 ${s3_nb_hour_dir} ${NOBID_HOUR_DIR}" 
    ${HADOOP} distcp ${CONF_PROD} ${qName} -Ddistcp.bytes.per.map=102400 ${s3_nb_hour_dir} ${NOBID_HOUR_DIR} 
    if [ $? -eq 0 ]
    then
        ${HADOOP} fs -touchz ${NOBID_HOUR_DIR}/_SUCCESS
        ${HADOOP} fs -rm -r ${NOBID_BASE_DIR}/_distcp*
        echo "Copying the nobid data from ${s3_nb_hour_dir} to ${NOBID_HOUR_DIR} is done."
    else              
        msg="Copying nobid data job failed. Please investigate!"
        sendalert $msg
    fi
fi 

#exit 0

