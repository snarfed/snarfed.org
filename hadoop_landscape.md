
Hadoop landscape survey

[Hadoop](https://hadoop.apache.org/), the venerable distributed compute framework, has long been _the_ dominant open source platform for data processing. It's survived as long as it has by embracing and extending new technologies like Spark, Kubernetes, Ozone, and Presto as they've come along. Its ecosystem is broad, deep, and mature. Building on it has generally been a solid decision.

Here's a very brief survey of the Hadoop landscape, as of mid 2021.

## Key components

- **Compute:** MapReduce is the old trusty war horse here. Simple, easy to use, limited expressivity. Not easy to define rich compute graphs and dependencies. Slowly getting replace by Spark (batch) and Flink (stream).
- **Cluster/resource mgmt:** YARN is the legacy offering. Will stick around for a while, but already getting replaced by K8s.
- **Storage:** HDFS, legacy distributed file system. Similarly straightforward and usable, extremely widely adopted and supported, but losing ground to more modern object stores (eg Ozone) and adapters for big three cloud storage (S3, EMRFS, etc).
- **Other:** pubsub aka event bus (Kafka), data catalog and warehouse (Hive), BI query engine (Presto), and many more we don't currently use or need.

## Providers and distributions

From the big three cloud providers:

- AWS EMR
- GCP Dataproc
- Azure HDInsight

Other:

- Cloudera (acquired HortonWorks)
- Qubole
- Oracle, IBM, others

## Levels of administration

- **Self hosted open source:** spin up VMs (or bare metal servers), install and configure and run the various Hadoop components yourself.
- **Self hosted distribution:** same but install a distribution from Cloudera or similar instead. Streamlines config, integration.
- **Managed:** a provider like AWS provides low level resources (compute instances, storage, etc) and offers tools for configuring clusters and running jobs.
- **High level:** like managed, but much higher level UX, eg Qubole Data Service, Ambari, DataBricks, etc.

## Alternatives

Hadoop doesn't have many direct competitors, at least not comparable open source data processing suites. Directly comparable MapReduce implementations include Disco and Cluster Map Reduce (inactive) and a handful of NoSQL stores: MongoDB, CouchDB, Riak.

Thinner open source frameworks include Airflow, Kubeflow, Hydra, Luigi, [and more.](https://github.com/pditommaso/awesome-pipeline#pipeline-frameworks--libraries)

The closed source world has more direct corollaries. AWS, Azure, and GCP have some comparable suites, eg GCP Dataflow and AWS SageMaker. There are lots of other commercial offerings too, notably DataBricks, Pachyderm, maybe Snowflake.

The scientific computing world has HPCC and similar MPI-based platforms, but those are niche and not generally applicable.

## Conclusions

If you're a legacy software shop on MapReduce-based Hadoop, there are lots of improvement opportunities, but probably nothing immediately burning, which is fortunate. Here are some Hadoop-specific recommendations, from small to big:

- Choose a single, long-lived, always-on, autoscaled cluster that makes running your primary workloads easy and other workloads at least possible.
- If you're on MapReduce, consider migrating to Spark.
- Consolidate down to a single cloud, if possible. Hedge lock-in with containers, K8s, and other agnostic tools, but most companies should avoid building custom multi-cloud layers.
- Consider switching from manual cloud storage integration (S3 etc) to distributed filesystems like EMRFS. (Probably _not_ HDFS though.)
- If you do data science or machine learning, invest in tooling that let data scientists easily run experiments and analyses on the cluster. One approach here is an environment that knits together local and cluster, where they can iterate on code interactively (eg in a Python or R shell) that automatically runs each expression locally if possible, otherwise on the cluster, with per-expression bootstrap latency on the order of seconds at most. PySpark on GCP Dataproc is an example.
