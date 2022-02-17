import {Message} from 'google-protobuf';
import {EventComplete, EventProgress, EventRequest} from './proto/sipc_frames_pb';

export class ProtoMessageHouse {
  public static getTypeUrl(data: Message): string {
    let fullName = '';
    if (data instanceof EventRequest) {
      fullName = 'jcu.sipc.proto.EventRequest';
    } else if (data instanceof EventProgress) {
      fullName = 'jcu.sipc.proto.EventProgress';
    } else if (data instanceof EventComplete) {
      fullName = 'jcu.sipc.proto.EventComplete';
    }
    return `pb-type.jc-lab.net/sipc/${fullName}`
  }
}
