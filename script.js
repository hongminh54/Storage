const apiUrl = 'https://api.github.com/repos/VoChiDanh/Storage/commits';

// Replace {owner} with the GitHub repository owner's username or organization name
// Replace {repo} with the GitHub repository name

const changelogList = document.getElementById('changelogList');

function fetchCommits() {
  fetch(apiUrl)
    .then(response => response.json())
    .then(commits => {
      const changelogMap = new Map();
      let previousVersion = null;

      commits.forEach(commit => {
        const message = commit.commit.message;
        const versionRegex = /^(\d+\.\d+\.\d+)/;
        const changelogRegex = /^(\d+\.\d+\.\d+)\s*-\s*(.+)/;

        let version = null;
        let changelog = message;

        if (changelogRegex.test(message)) {
          [, version, changelog] = message.match(changelogRegex);
        } else if (versionRegex.test(message)) {
          [version] = message.match(versionRegex);
        }

        if (!version) {
          if (previousVersion) {
            if (versionRegex.test(previousVersion)) {
              version = previousVersion.endsWith("-SNAPSHOT") ? previousVersion : previousVersion + "-SNAPSHOT";
            } else {
              version = previousVersion;
            }
          } else {
            version = "Build " + commit.sha.substring(0, 7);
          }
        }

        previousVersion = version;

        const commitDate = new Date(commit.commit.committer.date);
        const formattedDate = commitDate.toLocaleDateString();

        const changelogWithDate = `${changelog} (${formattedDate})`; // Append the commit date to the changelog

        if (changelogMap.has(version)) {
          changelogMap.get(version).push(changelogWithDate);
        } else {
          changelogMap.set(version, [changelogWithDate]);
        }
      });

      changelogMap.forEach((changelogs, version) => {
        const listItem = document.createElement('li');
        listItem.classList.add('changelog-item');

        const versionHeader = document.createElement('h3');
        versionHeader.classList.add('version-header');
        versionHeader.textContent = `${version}`;
        listItem.appendChild(versionHeader);

        if (changelogs.length > 0) {
          const changelogList = document.createElement('ul');
          changelogList.classList.add('changelog-sublist');
          changelogs.forEach(changelog => {
            const changelogItem = document.createElement('li');
            changelogItem.classList.add('changelog-subitem');
            changelogItem.textContent = changelog;
            changelogList.appendChild(changelogItem);
          });
          listItem.appendChild(changelogList);
        } else {
          const noChangelogMessage = document.createElement('p');
          noChangelogMessage.classList.add('no-changelog-message');
          noChangelogMessage.textContent = 'No changelog available.';
          listItem.appendChild(noChangelogMessage);
        }

        changelogList.appendChild(listItem);
      });
    })
    .catch(error => {
      console.error('Error:', error);
    });
}

fetchCommits();