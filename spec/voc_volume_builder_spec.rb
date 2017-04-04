require 'rest-client'
require 'json'

describe 'Given testing voc weighted volume numbers for accuracy' do
  OpenSSL::SSL::VERIFY_PEER = OpenSSL::SSL::VERIFY_NONE

  before :all do
    @support_tracks = 'customfield_13091'
    @support_weekly_average = 'customfield_13491'
    @valid_support_tracks = '<span class="aui-lozenge aui-lozenge-subtle aui-lozenge-current">130</span>'
  end

  it 'should give back the voc weighted volume of 130s' do
    issue_key = 'EMM-4010'

    response = RestClient::Resource.new("https://devjira.roving.com/rest/api/latest/issue/#{issue_key}?fields=issuelinks").get

    json_response = JSON.parse(response.body)

    issue_ids = Array.new

    json_response['fields']['issuelinks'].each do |var|
      issue_ids.push var['outwardIssue']['self'] if var['outwardIssue']['key'].include? 'SRQ'
    end

    issue_ids.size.should eql(3)

    @support_tracks_array = Array.new
    @support_weekly_average_array = Array.new
    issue_ids.each do |issue|
      response = RestClient::Resource.new(issue).get
      json_response = JSON.parse(response.body)

      @support_tracks_array.push json_response['fields'][@support_tracks] unless json_response['fields'][@support_tracks].nil?
      @support_weekly_average_array.push json_response['fields'][@support_weekly_average] unless json_response['fields'][@support_weekly_average].nil?
    end

    support_tracks = @support_tracks_array.inject{|sum,x| sum + x }

    weekly_average = @support_weekly_average_array.inject{|sum,x| sum + x }

    support_tracks.should eq(130)
    weekly_average.should eq(1.25)

    response = RestClient::Resource.new('https://devjira.roving.com/plugins/servlet/vocvolume?jqlQuery=issuetype+not+in+(%22VOC+Request%22%2C%22Support+Request%22)&jqlGadget=issuekey=%27emm-4010%27&noTotal=true&escapingOnly=false&sortTotal=false&sortVocVolume=TRUE&numIssuesLimit=100&numResultsLimit=100', headers => {:'Authorization' => 'Basic cWEtc2VsZW5pdW06bzNWRVhyeFY='}).get


    response.body.should include(@valid_support_tracks)
  end
end