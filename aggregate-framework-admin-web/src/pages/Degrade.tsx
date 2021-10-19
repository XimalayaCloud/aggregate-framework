import React, {useEffect, useState} from 'react';
import {Col, Divider, Row, Switch} from 'antd';
import {degrade, getDegradeList} from '../common/api';

const Page = () => {
  const [domains, setDomains] = useState<any[]>([]);

  useEffect(() => {
    getDegradeList().then(data => {
      const _domains: any[] = [];
      Object.keys(data).forEach(key => {
        _domains.push({
          label: key,
          checked: data[key]
        })
      })
      setDomains(_domains);
    });
  }, []);

  const doDegrade = (domain: string, checked: boolean) => {
    degrade(domain, checked).then(res => {
      getDegradeList().then(data => {
        const _domains: any[] = [];
        Object.keys(data).forEach(key => {
          _domains.push({
            label: key,
            checked: data[key]
          })
        })
        setDomains(_domains);
      });
    });
  }

  return (
    <>
      <div className="tab-3-body">
        
        <Row>
          <Col flex="auto" style={{ paddingLeft: 12, fontWeight: 'bolder' }}>Domain</Col>
          <Col flex="200px" style={{ fontWeight: 'bolder' }}>状态</Col>
        </Row>
        <Divider orientation="left"></Divider>
        {
          domains.map((item, index) => (
            <>
              <Row key={`tab3_switch_${index}`}>
                <Col flex="auto" style={{ paddingLeft: 12 }}>{item.label}</Col>
                <Col flex="200px">
                  <Switch
                    checkedChildren="降级"
                    unCheckedChildren="正常"
                    checked={item.checked}
                    onChange={checked => {
                      console.log(checked, item.label)
                      doDegrade(item.label, checked);
                    }}
                  />
                </Col>
              </Row>
              <Divider orientation="left"></Divider>
            </>
          ))
        }
      </div>
    </>
  )
}

export default Page;