# moonlight

This project tackles the problem of data lineage in ETL (Extract Transform Load) processes.
It enables tracking of metadata related to data movements which helps answering the questions
such as:
- From which sources did the observed data originate?
- Which downstream data sources would be affected by changes in chosen dataset?
- Who owns particular parts of the pipeline?
- Why does this specific transformation is being done at all?
- Which systems are processing the data?
- Are there any metrics and alerts used for tracking the performance of these jobs?
- Where is the source code for these operations?
- When was the selected ETL ran last time and who did it?

These and similar questions are quite often hard to answer in architectures with lot of interlaced ETL jobs.
The goal of this project is to make the process of getting the required answers as easy as possible by
implementing a REST API for saving and extracting ETL metadata which is easy to use and deploy in the target system,
as well as a set of libraries for its usage (still a TODO item though).

Its intention is to help data engineers and everyone else working with data pipelines to better
understand how data is moving through their pipelines and to find and remove problems in them faster.

**NOTES**:
- This project is still in early development phase, so not yet ready for production usage.
- See the list of issues for the scheduled list of features, TODOs and bugs.

## Structure

This is a multi module project, which consists out of several projects:
1. `moonlight-core` - Core module which provides REST API for storage and retrieval of lineage metadata
2. `moonlight-data-model` - Contains data access layer and case classes for DTOs
3. TODO: `moonlight-client` - Scala Library for easy usage of the API

For more information of each of the modules check their `README.md` file.

## Contribution

For contributing the project create a PR containing the proposed changes.
<br>
**NOTE**: This is the first draft of the contribution methods, and probably will
be refined in the future.

## Licence

This project is released under *Apache License Version 2.0, January 2004*.
