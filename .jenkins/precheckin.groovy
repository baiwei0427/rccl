#!/usr/bin/env groovy

// Copyright (c) 2020-2023 Advanced Micro Devices, Inc. All rights reserved.
// This shared library is available at https://github.com/ROCmSoftwarePlatform/rocJENKINS/
@Library('rocJenkins@pong') _

// This is file for internal AMD use.
// If you are interested in running your own Jenkins, please raise a github issue for assistance.

import com.amd.project.*
import com.amd.docker.*
import java.nio.file.Path

def runCI =
{
    nodeDetails, jobName->

    def prj  = new rocProject('rccl', 'PreCheckin')

    prj.timeout.test = 300
    prj.paths.build_command = './install.sh -t --fast'

    // Define test architectures, optional rocm version argument is available
    def nodes = new dockerNodes(nodeDetails, jobName, prj)

    boolean formatCheck = false

    def commonGroovy

    def compileCommand =
    {
        platform, project->

        commonGroovy = load "${project.paths.project_src_prefix}/.jenkins/common.groovy"
        commonGroovy.runCompileCommand(platform, project, jobName)
    }

    def testCommand =
    {
        platform, project->

        commonGroovy.runTestCommand(platform, project, "*", "UT_POW2_GPUS=1")
    }

    def packageCommand =
    {
        platform, project->

        commonGroovy.runPackageCommand(platform, project, jobName)
    }

    buildProject(prj, formatCheck, nodes.dockerArray, compileCommand, testCommand, packageCommand)
}

ci: {
    String urlJobName = auxiliary.getTopJobName(env.BUILD_URL)

    def propertyList = ["compute-rocm-dkms-no-npi-hipclang":[pipelineTriggers([cron('0 1 * * 0')])]]

    propertyList = auxiliary.appendPropertyList(propertyList)

    def jobNameList = ["compute-rocm-dkms-no-npi-hipclang":([sles15sp1:['4gfx906'],centos8:['8gfx908'],centos7:['8gfx906'],ubuntu18:['4gfx906', '4gfx908']])]

    jobNameList = auxiliary.appendJobNameList(jobNameList)

    propertyList.each
    {
        jobName, property->
        if (urlJobName == jobName)
            properties(auxiliary.addCommonProperties(property))
    }

    jobNameList.each
    {
        jobName, nodeDetails->
        if (urlJobName == jobName)
            stage(jobName) {
                runCI(nodeDetails, jobName)
            }
    }

    // For url job names that are not listed by the jobNameList i.e. compute-rocm-dkms-no-npi-1901
    if(!jobNameList.keySet().contains(urlJobName))
    {
        properties(auxiliary.addCommonProperties([pipelineTriggers([cron('0 1 * * *')])]))
        stage(urlJobName) {
            runCI([ubuntu18:['4gfx906']], urlJobName)
        }
    }
}
