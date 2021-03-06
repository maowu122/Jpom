# The MIT License (MIT)
#
# Copyright (c) 2019 码之科技工作室
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of
# this software and associated documentation files (the "Software"), to deal in
# the Software without restriction, including without limitation the rights to
# use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
# the Software, and to permit persons to whom the Software is furnished to do so,
# subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
# FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
# COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
# IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#!/bin/bash

Tag="KeepBx-System-JpomServerApplication"
MainClass="cn.keepbx.jpom.JpomServerApplication"
# 自动获取当前路径
Path=$(cd `dirname $0`; pwd)"/"
Lib="${Path}lib/"
Log="${Path}server.log"
LogBack="${Path}log/"
JVM="-server "
# 修改项目端口号 日志路径
ARGS="--jpom.applicationTag=${Tag} --server.port=2122  --jpom.log=${Path}log"

echo ${Tag}
echo ${Path}
RETVAL="0"

# 启动程序
function start() {
    if [[ -z "${JAVA_HOME}" ]] ; then
        echo "请配置【JAVA_HOME】环境变量"
        exit 2
    fi
    echo  ${Log}
    # 备份日志
    if [[ -f ${Log} ]]; then
		if [[ ! -d ${LogBack} ]];then
			mkdir ${LogBack}
		fi
			cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`.log"
			mv ${Log}  ${LogBack}${cur_dateTime}
			echo "mv to $LogBack$cur_dateTime"
		touch ${Log}
	fi
	# classPath
    CLASSPATH=`listDir ${Lib}`
    nohup java  ${JVM} -classpath ${CLASSPATH}${JAVA_HOME}/lib/tools.jar -Dapplication=${Tag} -Dbasedir=${Path} ${MainClass} ${ARGS}  >> ${Log} 2>&1 &
    if [[ -f ${Log} ]]; then
        tail -f ${Log}
    else
        sleep 3
        if [[  -f ${Log} ]]; then
           tail -f ${Log}
        else
           echo "还没有生成日志文件:${Log}"
        fi
    fi
}

# 拼接所有文件
function listDir()
{
    ALL=""
	for file in `ls $1`
	do
		#得到文件的完整的目录
        ALL="${ALL}${1}/${file}:"
	done
	echo ${ALL}
}

# 停止程序
function stop() {
	pid=$(ps -ef | grep -v 'grep' | egrep ${Tag}| awk '{printf $2 " "}')
	if [[ "$pid" != "" ]]; then
        echo -n "boot ( pid $pid) is running" 
        echo 
        echo -n $"Shutting down boot: wait"
        kill $(pgrep -f ${Tag}) 2>/dev/null
        sleep 3
		pid=$(ps -ef | grep -v 'grep' | egrep ${Tag}| awk '{printf $2 " "}')
		if [[ "$pid" != "" ]]; then
			echo "kill boot process"
			kill -9 "$pid"
		fi
    else
        echo "boot is stopped"
    fi

	status
}

# 获取程序状态
function status()
{
	pid=$(ps -ef | grep -v 'grep' | egrep ${Tag}| awk '{printf $2 " "}')
	#echo "$pid"
	if [[ "$pid" != "" ]]; then
		echo "boot is running,pid is $pid"
	else
		echo "boot is stopped"
	fi
}

# 重新加载nginx
function reloadNginx(){
    nginx -t
    nginx -s reload
}

# 提示使用语法
function usage()
{
   echo "Usage: $0 {start|stop|restart|status|reloadNginx}"
   RETVAL="2"
}

# See how we were called.
RETVAL="0"
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        start
        ;;
    reloadNginx)
        reloadNginx
        ;;
    status)
        status
        ;;
    *)
      usage
      ;;
esac

exit $RETVAL
