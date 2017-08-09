#!/usr/bin/env groovy
// the "!#/usr/bin... is just to to help IDEs, GitHub diffs, etc properly detect the language and do syntax highlighting for you.
// thx to https://github.com/jenkinsci/pipeline-examples/blob/master/docs/BEST_PRACTICES.md

// note that we set a default version for this library in jenkins, so we don't have to specify it here
@Library('misc')
import de.metas.jenkins.MvnConf
import de.metas.jenkins.Misc

//
// setup: we'll need the following variables in different stages, that's we we create them here
//

// thx to http://stackoverflow.com/a/36949007/1012103 with respect to the paramters
properties([
	parameters([
		string(defaultValue: '',
			description: '''If this job is invoked via an updstream build job, then that job can provide either its branch or the respective <code>MF_UPSTREAM_BRANCH</code> that was passed to it.<br>
This build will then attempt to use maven dependencies from that branch, and it will sets its own name to reflect the given value.
<p>
So if this is a "master" build, but it was invoked by a "feature-branch" build then this build will try to get the feature-branch\'s build artifacts annd will set its
<code>currentBuild.displayname</code> and <code>currentBuild.description</code> to make it obvious that the build contains code from the feature branch.''',
			name: 'MF_UPSTREAM_BRANCH'),

		string(defaultValue: '',
			description: 'Build number of the upstream job that called us, if any.',
			name: 'MF_UPSTREAM_BUILDNO'),

		string(defaultValue: '',
			description: 'Version of the metasfresh "main" code we shall use when resolving dependencies. Leave empty and this build will use the latest.',
			name: 'MF_UPSTREAM_VERSION'),

		booleanParam(defaultValue: true, description: 'Set to true if this build shall trigger "endcustomer" builds.<br>Set to false if this build is called from elsewhere and the orchestrating also takes place elsewhere',
			name: 'MF_TRIGGER_DOWNSTREAM_BUILDS')
	]),
	pipelineTriggers([]),
	buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '20')) // keep the last 20 builds
])

final MF_UPSTREAM_BRANCH;
if(params.MF_UPSTREAM_BRANCH)
{
	echo "Setting MF_UPSTREAM_BRANCH from params.MF_UPSTREAM_BRANCH=${params.MF_UPSTREAM_BRANCH}"
	MF_UPSTREAM_BRANCH=params.MF_UPSTREAM_BRANCH
}
else
{
	echo "Setting MF_UPSTREAM_BRANCH from env.BRANCH_NAME=${env.BRANCH_NAME}"
	MF_UPSTREAM_BRANCH=env.BRANCH_NAME
}
if(params.MF_UPSTREAM_BUILDNO)
{
	echo "Setting MF_UPSTREAM_BUILDNO from params.MF_UPSTREAM_BUILDNO=${params.MF_UPSTREAM_BUILDNO}"
	MF_UPSTREAM_BUILDNO=params.MF_UPSTREAM_BUILDNO
}
else
{
	echo "Setting MF_UPSTREAM_BUILDNO from env.BUILD_NUMBER=${env.BUILD_NUMBER}"
	MF_UPSTREAM_BUILDNO=env.BUILD_NUMBER
}

// set the version prefix, 1 for "master", 2 for "not-master" a.k.a. feature
final MF_BUILD_VERSION_PREFIX = MF_UPSTREAM_BRANCH.equals('master') ? "1" : "2"
echo "Setting MF_BUILD_VERSION_PREFIX=$MF_BUILD_VERSION_PREFIX"

final MF_BUILD_VERSION=MF_BUILD_VERSION_PREFIX + "-" + env.BUILD_NUMBER;
echo "Setting MF_BUILD_VERSION=$MF_BUILD_VERSION"

currentBuild.displayName="${MF_UPSTREAM_BRANCH} - build #${currentBuild.number} - artifact-version ${MF_BUILD_VERSION}";
// note: going to set currentBuild.description after we deployed

timestamps
{
// https://github.com/metasfresh/metasfresh/issues/2110 make version/build infos more transparent
final String MF_RELEASE_VERSION = retrieveReleaseInfo(MF_UPSTREAM_BRANCH);
echo "Retrieved MF_RELEASE_VERSION=${MF_RELEASE_VERSION}"
final String MF_VERSION="${MF_RELEASE_VERSION}.${MF_BUILD_VERSION}";
echo "set MF_VERSION=${MF_VERSION}";

node('agent && linux') // shall only run on a jenkins agent with linux
{
    configFileProvider([configFile(fileId: 'metasfresh-global-maven-settings', replaceTokens: true, variable: 'MAVEN_SETTINGS')])
    {
    	// create our config instance to be used further on
    	final MvnConf mvnConf = new MvnConf(
    		'pom.xml', // pomFile
    		MAVEN_SETTINGS, // settingsFile
    		"mvn-${MF_UPSTREAM_BRANCH}", // mvnRepoName
    		'https://repo.metasfresh.com' // mvnRepoBaseURL
    	)
    	echo "mvnConf=${mvnConf}"

        withMaven(jdk: 'java-8', maven: 'maven-3.3.9', mavenLocalRepo: '.repository')
        {
				stage('Set versions and build metasfresh-webui-api')
        {

        checkout scm; // i hope this to do all the magic we need
        sh 'git clean -d --force -x' // clean the workspace

        nexusCreateRepoIfNotExists mvnConf.mvnDeployRepoBaseURL, mvnConf.mvnRepoName

        // update the parent pom version
        mvnUpdateParentPomVersion mvnConf

		final String mavenUpdatePropertyParam;
		if(params.MF_UPSTREAM_VERSION)
		{
			final inSquaresIfNeeded = { String version -> return version == "LATEST" ? version: "[${version}]"; }
			// update the property, use the metasfresh version that we were given by the upstream job.
			// the square brackets are required if we have a conrete version (i.e. not "LATEST"); see https://github.com/mojohaus/versions-maven-plugin/issues/141 for details
			mavenUpdatePropertyParam="-Dproperty=metasfresh.version -DnewVersion=${inSquaresIfNeeded(params.MF_UPSTREAM_VERSION)}";
		}
		else
		{
			// still update the property, but use the latest version
			mavenUpdatePropertyParam='-Dproperty=metasfresh.version';
		}

		// update the metasfresh.version property. either to the latest version or to the given params.MF_UPSTREAM_VERSION.
		sh "mvn --debug --settings ${mvnConf.settingsFile} --file ${mvnConf.pomFile} --batch-mode ${mvnConf.resolveParams} ${mavenUpdatePropertyParam} versions:update-property"

		// set the artifact version of everything below the webui's ${mvnConf.pomFile}
		sh "mvn --settings ${mvnConf.settingsFile} --file ${mvnConf.pomFile} --batch-mode -DnewVersion=${MF_VERSION} -DallowSnapshots=false -DgenerateBackupPoms=true -DprocessDependencies=false -DprocessParent=true -DexcludeReactor=true -Dincludes=\"de.metas.ui.web*:*\" ${mvnConf.resolveParams} versions:set"

		final BUILD_ARTIFACT_URL="${mvnConf.deployRepoURL}/de/metas/ui/web/metasfresh-webui-api/${MF_VERSION}/metasfresh-webui-api-${MF_VERSION}.jar";

		// do the actual building and deployment
		// maven.test.failure.ignore=true: continue if tests fail, because we want a full report.
		sh "mvn --settings ${mvnConf.settingsFile} --file ${mvnConf.pomFile} --batch-mode -Dmaven.test.failure.ignore=true ${mvnConf.resolveParams} ${mvnConf.deployParam} clean deploy"

		// now create and publish some docker image..well, 1 docker image for starts
		final dockerWorkDir='docker-build/metasfresh-webui-api'
		sh "mkdir -p ${dockerWorkDir}"

		def misc = new de.metas.jenkins.Misc();

		final BUILD_DOCKER_REPOSITORY='metasfresh';
		final BUILD_DOCKER_NAME='metasfresh-webapi-dev';
		final BUILD_DOCKER_TAG=misc.mkDockerTag("${MF_UPSTREAM_BRANCH}-${MF_VERSION}");
		final BUILD_DOCKER_IMAGE="${BUILD_DOCKER_REPOSITORY}/${BUILD_DOCKER_NAME}:${BUILD_DOCKER_TAG}";

		// create and upload a docker image
		sh "cp target/metasfresh-webui-api-${MF_VERSION}.jar ${dockerWorkDir}/metasfresh-webui-api.jar" // copy the file so it can be handled by the docker build
		sh "cp -R src/main/docker/* ${dockerWorkDir}"
		sh "cp -R src/main/configs ${dockerWorkDir}"
		docker.withRegistry('https://index.docker.io/v1/', 'dockerhub_metasfresh')
		{
			def app = docker.build "${BUILD_DOCKER_REPOSITORY}/${BUILD_DOCKER_NAME}", "${dockerWorkDir}";

			app.push misc.mkDockerTag("${MF_UPSTREAM_BRANCH}-latest");
			app.push BUILD_DOCKER_TAG;
			if(MF_UPSTREAM_BRANCH=='release')
			{
				echo 'MF_UPSTREAM_BRANCH=release, so we also push this with the "latest" tag'
				app.push misc.mkDockerTag('latest');
			}
		}

		// gh #968:
		// set env variables which will be available to a possible upstream job that might have called us
		// all those env variables can be gotten from <buildResultInstance>.getBuildVariables()
		// note: we do it here, because we also expect these vars to end up in the application.properties within our artifact
		env.BUILD_ARTIFACT_URL="${BUILD_ARTIFACT_URL}";
		env.BUILD_CHANGE_URL="${env.CHANGE_URL}";
		env.MF_BUILD_VERSION="${MF_BUILD_VERSION}";
		env.BUILD_GIT_SHA1="${misc.getCommitSha1()}";
		env.BUILD_DOCKER_IMAGE="${BUILD_DOCKER_IMAGE}";
		env.MF_VERSION="${MF_VERSION}"

		currentBuild.description="""This build's main artifacts (if not yet cleaned up) are
<ul>
<li>The executable jar <a href=\"${BUILD_ARTIFACT_URL}\">metasfresh-webui-api-${MF_VERSION}.jar</a></li>
<li>A docker image which you can run in docker via<br>
<code>docker run --rm -d -p 8080:8080 -e "DB_HOST=localhost" --name metasfresh-webui-api-${MF_VERSION} ${BUILD_DOCKER_IMAGE}</code></li>
</ul>"""
				junit '**/target/surefire-reports/*.xml'
      }
		}
	}
 } // node

if(params.MF_TRIGGER_DOWNSTREAM_BUILDS)
{
	stage('Invoke downstream job')
	{
   def misc = new de.metas.jenkins.Misc();
   final String jobName = misc.getEffectiveDownStreamJobName('metasfresh', MF_UPSTREAM_BRANCH);

   build job: jobName,
     parameters: [
       string(name: 'MF_UPSTREAM_BRANCH', value: MF_UPSTREAM_BRANCH),
       string(name: 'MF_UPSTREAM_BUILDNO', value: MF_UPSTREAM_BUILDNO),
       string(name: 'MF_UPSTREAM_VERSION', value: MF_BUILD_VERSION),
       string(name: 'MF_UPSTREAM_JOBNAME', value: 'metasfresh-webui'),
       booleanParam(name: 'MF_TRIGGER_DOWNSTREAM_BUILDS', value: false), // the job shall just run but not trigger further builds because we are doing all the orchestration
       booleanParam(name: 'MF_SKIP_TO_DIST', value: true) // this param is only recognised by metasfresh
     ], wait: false
	}
}
else
{
	echo "params.MF_TRIGGER_DOWNSTREAM_BUILDS=${params.MF_TRIGGER_DOWNSTREAM_BUILDS}, so we do not trigger any downstream builds"
}
} // timestamps
